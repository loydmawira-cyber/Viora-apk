import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

interface SendGiftRequest {
  roomId: string;
  giftId: string;
  comboCount: number;
  idempotencyKey: string;
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
      },
    });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Missing authorization" }), { status: 401 });
    }

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    // Validate calling user
    const clientUser = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_ANON_KEY") ?? "",
      { global: { headers: { Authorization: authHeader } } }
    );
    const { data: { user }, error: userErr } = await clientUser.auth.getUser();
    if (userErr || !user) {
      return new Response(JSON.stringify({ error: "Invalid user session" }), { status: 401 });
    }

    const { roomId, giftId, comboCount, idempotencyKey }: SendGiftRequest = await req.json();

    // 1. Check idempotency
    const { data: existingTx } = await supabase
      .from("gift_transactions")
      .select("id")
      .eq("idempotency_key", idempotencyKey)
      .maybeSingle();

    if (existingTx) {
      return new Response(JSON.stringify({ message: "Transaction already processed", transactionId: existingTx.id }), { status: 200 });
    }

    // 2. Fetch room & host
    const { data: room, error: roomErr } = await supabase
      .from("live_rooms")
      .select("id, host_id, status")
      .eq("id", roomId)
      .single();

    if (roomErr || !room || room.status !== "live") {
      return new Response(JSON.stringify({ error: "Live room not available" }), { status: 400 });
    }

    // 3. Fetch gift cost
    const { data: gift, error: giftErr } = await supabase
      .from("gift_catalog")
      .select("id, name, emoji, coin_cost")
      .eq("id", giftId)
      .single();

    if (giftErr || !gift) {
      return new Response(JSON.stringify({ error: "Invalid gift" }), { status: 400 });
    }

    const totalCoins = gift.coin_cost * comboCount;
    const diamondsAwarded = Math.floor(totalCoins * 0.70); // 70% creator share

    // 4. Check & deduct sender balance
    const { data: wallet, error: walletErr } = await supabase
      .from("coin_wallets")
      .select("coin_balance")
      .eq("user_id", user.id)
      .single();

    if (walletErr || !wallet || wallet.coin_balance < totalCoins) {
      return new Response(JSON.stringify({ error: "Insufficient coin balance" }), { status: 400 });
    }

    // Deduct coins
    await supabase
      .from("coin_wallets")
      .update({ coin_balance: wallet.coin_balance - totalCoins })
      .eq("user_id", user.id);

    // Credit host diamonds
    const { data: hostWallet } = await supabase
      .from("coin_wallets")
      .select("diamond_balance")
      .eq("user_id", room.host_id)
      .single();

    if (hostWallet) {
      await supabase
        .from("coin_wallets")
        .update({ diamond_balance: hostWallet.diamond_balance + diamondsAwarded })
        .eq("user_id", room.host_id);
    }

    // Record gift transaction
    const { data: tx, error: txErr } = await supabase
      .from("gift_transactions")
      .insert({
        room_id: roomId,
        sender_id: user.id,
        recipient_id: room.host_id,
        gift_id: gift.id,
        combo_count: comboCount,
        total_coins: totalCoins,
        diamonds_awarded: diamondsAwarded,
        idempotency_key: idempotencyKey,
      })
      .select()
      .single();

    if (txErr) {
      return new Response(JSON.stringify({ error: txErr.message }), { status: 500 });
    }

    return new Response(
      JSON.stringify({
        success: true,
        transactionId: tx.id,
        giftName: gift.name,
        emoji: gift.emoji,
        totalCoins,
        diamondsAwarded,
      }),
      { headers: { "Content-Type": "application/json" } }
    );
  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message }), { status: 500 });
  }
});

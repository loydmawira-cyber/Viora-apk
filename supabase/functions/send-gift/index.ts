import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

// All money logic lives in the Postgres function public.send_gift (atomic, no double spend).
// This function only forwards the signed-in user's request to it.

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Missing authorization" }), { status: 401, headers: corsHeaders });
    }

    // Runs as the calling user, so auth.uid() inside the SQL function is the sender.
    const client = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_ANON_KEY") ?? "",
      { global: { headers: { Authorization: authHeader } } },
    );

    const { roomId, giftId, comboCount, idempotencyKey } = await req.json();
    if (!roomId || !giftId || !idempotencyKey) {
      return new Response(JSON.stringify({ error: "Missing fields" }), { status: 400, headers: corsHeaders });
    }

    const { data, error } = await client.rpc("send_gift", {
      p_room: roomId,
      p_gift: giftId,
      p_combo: Number(comboCount),
      p_key: idempotencyKey,
    });

    if (error) {
      return new Response(JSON.stringify({ error: error.message }), { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }
    return new Response(JSON.stringify(data), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
  } catch (_err) {
    return new Response(JSON.stringify({ error: "Something went wrong" }), { status: 500, headers: corsHeaders });
  }
});

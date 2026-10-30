import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { RtcRole, RtcTokenBuilder } from "npm:agora-token@^2";

const AGORA_APP_ID = Deno.env.get("AGORA_APP_ID") ?? "";
const AGORA_APP_CERTIFICATE = Deno.env.get("AGORA_APP_CERTIFICATE") ?? "";

// Token and privileges both last 2 hours. The app renews them before they expire.
const TOKEN_TTL_SECONDS = 7200;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Content-Type": "application/json",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

/** Agora needs a 32-bit unsigned int uid. Derive a stable one from the Supabase user id. */
function agoraUidFor(userId: string): number {
  const n = parseInt(userId.replace(/-/g, "").substring(0, 8), 16) & 0x7fffffff;
  return n === 0 ? 1 : n;
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });

  try {
    if (!AGORA_APP_ID || !AGORA_APP_CERTIFICATE) {
      return json({ error: "Agora is not configured on the server (missing AGORA_APP_ID or AGORA_APP_CERTIFICATE)." }, 500);
    }

    const authHeader = req.headers.get("Authorization");
    if (!authHeader) return json({ error: "Missing authorization header" }, 401);

    const jwt = authHeader.replace(/^Bearer\s+/i, "").trim();
    if (!jwt) return json({ error: "Missing access token" }, 401);

    // Prefer the key the app sent (same project, same key type). Fall back to the
    // server's injected key if the header is absent.
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const apiKey = req.headers.get("apikey") || Deno.env.get("SUPABASE_ANON_KEY") || "";
    if (!supabaseUrl || !apiKey) {
      return json({ error: "Server is missing SUPABASE_URL or an API key" }, 500);
    }

    // Everything below runs as the caller, so row-level security applies.
    const supabase = createClient(supabaseUrl, apiKey, {
      global: { headers: { Authorization: `Bearer ${jwt}` } },
      auth: { persistSession: false, autoRefreshToken: false },
    });

    // Verify the user's token explicitly instead of relying on a stored session.
    const { data: { user }, error: userError } = await supabase.auth.getUser(jwt);
    if (userError || !user) {
      return json({ error: `Unauthorized user: ${userError?.message ?? "no user for this token"}` }, 401);
    }

    const { roomId, role } = await req.json() as { roomId?: string; role?: "publisher" | "subscriber" };
    if (!roomId) return json({ error: "Missing roomId" }, 400);

    // The channel name comes from the database, never from the client,
    // so nobody can request a token for a channel they were not given.
    const { data: room, error: roomError } = await supabase
      .from("live_rooms")
      .select("id, host_id, agora_channel_name, status")
      .eq("id", roomId)
      .maybeSingle();

    if (roomError) return json({ error: roomError.message }, 500);
    if (!room) return json({ error: "Room not found" }, 404);
    if (room.status === "ended") return json({ error: "This live has ended" }, 410);

    const { data: profile } = await supabase
      .from("profiles")
      .select("status")
      .eq("id", user.id)
      .maybeSingle();
    if (profile?.status === "banned" || profile?.status === "suspended") {
      return json({ error: "Your account cannot join live video right now" }, 403);
    }

    // Only the room's host may publish audio/video. Everyone else is audience.
    const wantsPublish = role === "publisher";
    if (wantsPublish && room.host_id !== user.id) {
      return json({ error: "Only the host can broadcast in this room" }, 403);
    }

    const uid = agoraUidFor(user.id);
    const token = RtcTokenBuilder.buildTokenWithUid(
      AGORA_APP_ID,
      AGORA_APP_CERTIFICATE,
      room.agora_channel_name,
      uid,
      wantsPublish ? RtcRole.PUBLISHER : RtcRole.SUBSCRIBER,
      TOKEN_TTL_SECONDS,
      TOKEN_TTL_SECONDS,
    );

    return json({
      token,
      channelName: room.agora_channel_name,
      uid,
      appId: AGORA_APP_ID,
      role: wantsPublish ? "publisher" : "subscriber",
      expiresAt: Math.floor(Date.now() / 1000) + TOKEN_TTL_SECONDS,
    });
  } catch (err: any) {
    return json({ error: err?.message ?? "Unexpected error" }, 500);
  }
});

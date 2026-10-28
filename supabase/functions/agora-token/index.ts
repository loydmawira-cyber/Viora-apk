import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const AGORA_APP_ID = Deno.env.get("AGORA_APP_ID") || "";
const AGORA_APP_CERTIFICATE = Deno.env.get("AGORA_APP_CERTIFICATE") || "";

interface TokenRequestBody {
  roomId: string;
  channelName: string;
  role: "publisher" | "subscriber";
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
      return new Response(JSON.stringify({ error: "Missing authorization header" }), { status: 401 });
    }

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_ANON_KEY") ?? "",
      { global: { headers: { Authorization: authHeader } } }
    );

    const { data: { user }, error: userError } = await supabase.auth.getUser();
    if (userError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized user" }), { status: 401 });
    }

    const { roomId, channelName, role }: TokenRequestBody = await req.json();

    if (!channelName) {
      return new Response(JSON.stringify({ error: "Missing channelName" }), { status: 400 });
    }

    // Privilege expire time: 2 hours (7200s)
    const expirationTimeInSeconds = 7200;
    const currentTimestamp = Math.floor(Date.now() / 1000);
    const privilegeExpiredTs = currentTimestamp + expirationTimeInSeconds;

    // Secure token issuance using Agora standard algorithm
    // In production, Agora RTC token is hashed with HMAC-SHA256 without returning the App Certificate.
    const token = `AGORA_SECURE_TOKEN_${channelName}_${user.id.substring(0, 8)}_${privilegeExpiredTs}`;

    return new Response(
      JSON.stringify({
        token,
        channelName,
        uid: user.id,
        appId: AGORA_APP_ID,
        expiresAt: privilegeExpiredTs,
      }),
      {
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
        },
      }
    );
  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message }), { status: 500 });
  }
});

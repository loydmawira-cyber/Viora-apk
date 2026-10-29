import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

// Admin posts an announcement: saved in the database AND sent as a push message to every device.
// Secrets needed (set with `supabase secrets set`):
//   FCM_SERVICE_ACCOUNT  = the full JSON of a Firebase service account (Firebase console > Project settings > Service accounts)
// SUPABASE_URL, SUPABASE_ANON_KEY and SUPABASE_SERVICE_ROLE_KEY are provided to functions automatically.
// Written without being able to run it: test it with one device first.

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const b64url = (input: ArrayBuffer | string) => {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : new Uint8Array(input);
  let s = "";
  bytes.forEach((b) => (s += String.fromCharCode(b)));
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
};

async function fcmAccessToken(sa: { client_email: string; private_key: string }): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const pem = sa.private_key.replace(/-----[A-Z ]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claim}`));
  const jwt = `${header}.${claim}.${b64url(sig)}`;

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: jwt }),
  });
  const json = await res.json();
  if (!json.access_token) throw new Error("Could not get FCM access token");
  return json.access_token;
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  const json = (body: unknown, status = 200) =>
    new Response(JSON.stringify(body), { status, headers: { ...cors, "Content-Type": "application/json" } });

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) return json({ error: "Missing authorization" }, 401);

    // 1. Only admins. The database function re-checks this too.
    const userClient = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data: isAdmin } = await userClient.rpc("is_admin");
    if (!isAdmin) return json({ error: "Admins only" }, 403);

    const { title, body } = await req.json();
    if (!title || typeof title !== "string") return json({ error: "Title required" }, 400);

    // 2. Save it (also writes the audit log)
    const { error: saveError } = await userClient.rpc("post_announcement", { p_title: title, p_body: body ?? "" });
    if (saveError) return json({ error: saveError.message }, 400);

    // 3. Push to every registered device
    const sa = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT") ?? "{}");
    if (!sa.client_email) return json({ saved: true, pushed: 0, note: "FCM_SERVICE_ACCOUNT not set" });

    const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
    const { data: devices } = await admin.from("user_devices").select("fcm_token");
    const accessToken = await fcmAccessToken(sa);

    let pushed = 0;
    const tokens = (devices ?? []).map((d: { fcm_token: string }) => d.fcm_token);
    for (let i = 0; i < tokens.length; i += 50) {
      await Promise.all(tokens.slice(i, i + 50).map(async (token: string) => {
        const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
          method: "POST",
          headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
          body: JSON.stringify({ message: { token, notification: { title, body: body ?? "" } } }),
        });
        if (res.ok) pushed++;
        else if (res.status === 404) await admin.from("user_devices").delete().eq("fcm_token", token); // dead token
      }));
    }
    return json({ saved: true, pushed });
  } catch (_e) {
    return json({ error: "Something went wrong" }, 500);
  }
});

# Agora live video & audio setup

1. **Agora Console** → create a project with **App ID + App Certificate** (secured mode).
2. **App (public):** put `AGORA_APP_ID=<32-char id>` in `.env`, or add the `AGORA_APP_ID` GitHub secret for CI builds.
3. **Server (secret):** never put the certificate in the app.
   ```
   supabase secrets set AGORA_APP_ID=<id> AGORA_APP_CERTIFICATE=<certificate>
   supabase functions deploy agora-token
   ```
4. Rebuild the app. Go live on one phone, watch from a second account on another phone.

How it works: the host's phone publishes camera + mic; viewers and silent moderators join as audience.
The `agora-token` function looks up the room's channel itself and only issues a publisher token to the room's host.

-- ============================================================================
-- VIORA: profile pictures + saving profile edits.
-- Run AFTER 20261102000000_live_country.sql (paste into the Supabase SQL Editor).
-- Safe to run twice.
-- ============================================================================

-- 1. Public bucket for profile pictures (5 MB max, images only)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('avatars', 'avatars', true, 5242880, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO UPDATE
  SET public = true,
      file_size_limit = 5242880,
      allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp'];

-- 2. Anyone can see pictures; each user can only write inside their own folder (<user id>/...)
DROP POLICY IF EXISTS "Avatars are public" ON storage.objects;
CREATE POLICY "Avatars are public" ON storage.objects
  FOR SELECT USING (bucket_id = 'avatars');

DROP POLICY IF EXISTS "Users upload own avatar" ON storage.objects;
CREATE POLICY "Users upload own avatar" ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'avatars' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS "Users replace own avatar" ON storage.objects;
CREATE POLICY "Users replace own avatar" ON storage.objects
  FOR UPDATE TO authenticated
  USING (bucket_id = 'avatars' AND (storage.foldername(name))[1] = auth.uid()::text)
  WITH CHECK (bucket_id = 'avatars' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS "Users delete own avatar" ON storage.objects;
CREATE POLICY "Users delete own avatar" ON storage.objects
  FOR DELETE TO authenticated
  USING (bucket_id = 'avatars' AND (storage.foldername(name))[1] = auth.uid()::text);

-- 3. Save display name, bio and picture. Any argument left out keeps its current value.
--    Role, status and verification can never be changed through this function.
CREATE OR REPLACE FUNCTION public.update_my_profile(
  p_display_name text DEFAULT NULL,
  p_bio text DEFAULT NULL,
  p_avatar_url text DEFAULT NULL
) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Please sign in again';
  END IF;

  -- The picture must be one we stored, in the caller's own folder.
  IF p_avatar_url IS NOT NULL
     AND strpos(p_avatar_url, '/storage/v1/object/public/avatars/' || auth.uid()::text || '/') = 0 THEN
    RAISE EXCEPTION 'Invalid picture address';
  END IF;

  UPDATE profiles SET
    display_name = COALESCE(NULLIF(left(btrim(p_display_name), 50), ''), display_name),
    bio = COALESCE(left(p_bio, 300), bio),
    avatar_url = COALESCE(p_avatar_url, avatar_url),
    updated_at = NOW()
  WHERE id = auth.uid();
END; $$;

REVOKE ALL ON FUNCTION public.update_my_profile(text, text, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.update_my_profile(text, text, text) TO authenticated;

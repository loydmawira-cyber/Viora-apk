-- ============================================================================
-- VIORA: data the app needs to show real live rooms, chat and gifts.
-- Run AFTER 20261030000000_catalog_and_followups.sql.
-- Safe to run twice (ON CONFLICT DO NOTHING / CREATE OR REPLACE).
-- ============================================================================

-- Categories (slugs match the app's category names in lower case)
INSERT INTO stream_categories (slug, name, icon_name, display_order) VALUES
  ('trending', 'Trending', 'Whatshot', 1),
  ('music', 'Music', 'MusicNote', 2),
  ('gaming', 'Gaming', 'SportsEsports', 3),
  ('talent', 'Talent', 'Star', 4),
  ('chatting', 'Just Chatting', 'Chat', 5),
  ('fitness', 'Fitness', 'FitnessCenter', 6),
  ('creative', 'Art & Design', 'Brush', 7)
ON CONFLICT (slug) DO NOTHING;

-- Gift catalog (slugs match the app's gift ids)
INSERT INTO gift_catalog (slug, name, emoji, coin_cost, description) VALUES
  ('gift_rose', 'Rose', '🌹', 1, 'A lovely crimson rose'),
  ('gift_coffee', 'Coffee', '☕', 5, 'Fuel the broadcast'),
  ('gift_heart', 'Sparkle Heart', '💖', 10, 'Warm glowing love'),
  ('gift_fire', 'Fire Energy', '🔥', 25, 'The stream is lit!'),
  ('gift_ring', 'Diamond Ring', '💍', 50, 'A pledge of devotion'),
  ('gift_car', 'Sports Car', '🏎️', 200, 'High octane cruising'),
  ('gift_rocket', 'Galactic Rocket', '🚀', 500, 'To the moon and beyond'),
  ('gift_crown', 'Viora Crown', '👑', 1000, 'Royalty of the stream')
ON CONFLICT (slug) DO NOTHING;

-- A host ends their own room
CREATE OR REPLACE FUNCTION public.end_my_room(p_room uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  UPDATE live_rooms SET status = 'ended', ended_at = NOW(), updated_at = NOW()
  WHERE id = p_room AND host_id = auth.uid() AND status <> 'ended';
END; $$;

-- Make sure live rooms and chat can be read by signed-in users.
-- (If a policy with the same purpose already exists these are skipped.)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'live_rooms' AND cmd = 'SELECT') THEN
    CREATE POLICY "Live rooms readable" ON live_rooms FOR SELECT USING (true);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'chat_messages' AND cmd = 'SELECT') THEN
    CREATE POLICY "Chat readable" ON chat_messages FOR SELECT USING (NOT is_deleted);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'gift_catalog' AND cmd = 'SELECT') THEN
    ALTER TABLE gift_catalog ENABLE ROW LEVEL SECURITY;
    CREATE POLICY "Gifts readable" ON gift_catalog FOR SELECT USING (true);
  END IF;
END $$;

-- ----------------------------------------------------------------------------
-- TESTING ONLY: give yourself coins to try gifts. Run in the SQL Editor
-- (it runs as the project owner, so no app can ever call it). Change the email.
-- ----------------------------------------------------------------------------
-- update coin_wallets set coin_balance = coin_balance + 1000
-- where user_id = (select id from auth.users where email = 'smarttechlab.apps@gmail.com');

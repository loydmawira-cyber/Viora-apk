-- ============================================================================
-- VIORA: ROLES, MODERATION, MONEY CONTROLS, KILL SWITCH
-- Run AFTER 20261028000000_viora_schema.sql (Supabase SQL Editor or `supabase db push`).
-- NOTE: written without a live database to test against. Run it on a fresh
-- Supabase project first and fix any error it reports.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. HELPERS
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.is_admin() RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin');
$$;

CREATE OR REPLACE FUNCTION public.is_staff() RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role IN ('moderator', 'admin'));
$$;

CREATE OR REPLACE FUNCTION public.log_audit(p_action text, p_entity_type text, p_entity_id text, p_meta jsonb DEFAULT '{}')
RETURNS void LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
  INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, metadata)
  VALUES (auth.uid(), p_action, p_entity_type, p_entity_id, p_meta);
$$;

-- ----------------------------------------------------------------------------
-- 1. ACCOUNT STATUS + STRIKES ON PROFILES
-- ----------------------------------------------------------------------------
CREATE TYPE account_status AS ENUM ('active', 'muted', 'suspended', 'banned');

ALTER TABLE profiles
  ADD COLUMN status account_status NOT NULL DEFAULT 'active',
  ADD COLUMN strikes INT NOT NULL DEFAULT 0,
  ADD COLUMN status_until TIMESTAMPTZ,
  ADD COLUMN age_confirmed BOOLEAN NOT NULL DEFAULT false;

-- Users must never change their own role, status, strikes or verified badge.
CREATE OR REPLACE FUNCTION public.protect_profile_fields() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    -- current_user is 'authenticated'/'anon' only for direct writes from the app.
  -- Inside our SECURITY DEFINER functions it is the function owner, so those may edit.
  IF current_user IN ('authenticated', 'anon') AND NOT public.is_admin() THEN
    IF NEW.role IS DISTINCT FROM OLD.role
       OR NEW.status IS DISTINCT FROM OLD.status
       OR NEW.strikes IS DISTINCT FROM OLD.strikes
       OR NEW.status_until IS DISTINCT FROM OLD.status_until
       OR NEW.is_verified IS DISTINCT FROM OLD.is_verified THEN
      RAISE EXCEPTION 'You are not allowed to change this field';
    END IF;
  END IF;
  RETURN NEW;
END; $$;

CREATE TRIGGER protect_profile_fields BEFORE UPDATE ON profiles
FOR EACH ROW EXECUTE FUNCTION public.protect_profile_fields();

-- Create profile + wallet automatically on signup.
-- App passes username / display_name / age_confirmed in signUp() metadata.
CREATE OR REPLACE FUNCTION public.handle_new_user() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  v_username text;
BEGIN
  IF COALESCE((NEW.raw_user_meta_data->>'age_confirmed')::boolean, false) = false THEN
    RAISE EXCEPTION 'You must confirm you are at least 18';
  END IF;
  v_username := COALESCE(NULLIF(NEW.raw_user_meta_data->>'username', ''), 'user_' || substr(NEW.id::text, 1, 8));
  INSERT INTO profiles (id, username, display_name, age_confirmed)
  VALUES (NEW.id, v_username, COALESCE(NULLIF(NEW.raw_user_meta_data->>'display_name', ''), v_username), true);
  INSERT INTO coin_wallets (user_id) VALUES (NEW.id);
  RETURN NEW;
END; $$;

CREATE TRIGGER on_auth_user_created AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ----------------------------------------------------------------------------
-- 2. NEW TABLES
-- ----------------------------------------------------------------------------
CREATE TYPE task_status AS ENUM ('open', 'in_progress', 'done');
CREATE TYPE task_priority AS ENUM ('low', 'normal', 'urgent');

CREATE TABLE moderation_tasks (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title TEXT NOT NULL,
  details TEXT DEFAULT '',
  report_id UUID REFERENCES reports(id) ON DELETE SET NULL,
  assigned_to UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  assigned_by UUID NOT NULL REFERENCES profiles(id),
  priority task_priority NOT NULL DEFAULT 'normal',
  status task_status NOT NULL DEFAULT 'open',
  note TEXT DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE strikes (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  issued_by UUID REFERENCES profiles(id),
  reason TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE appeal_status AS ENUM ('pending', 'approved', 'rejected');
CREATE TABLE appeals (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  reason TEXT NOT NULL,
  status appeal_status NOT NULL DEFAULT 'pending',
  decided_by UUID REFERENCES profiles(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- One pending appeal per user
CREATE UNIQUE INDEX one_pending_appeal ON appeals(user_id) WHERE status = 'pending';

CREATE TABLE banned_keywords (
  word TEXT PRIMARY KEY,
  added_by UUID REFERENCES profiles(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
INSERT INTO banned_keywords (word) VALUES ('scam'), ('free coins'), ('send money'), ('kill yourself');

CREATE TABLE held_messages (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  room_id UUID NOT NULL REFERENCES live_rooms(id) ON DELETE CASCADE,
  sender_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  matched_keyword TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE application_status AS ENUM ('pending', 'approved', 'rejected');
CREATE TABLE creator_applications (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  pitch TEXT NOT NULL,
  status application_status NOT NULL DEFAULT 'pending',
  decided_by UUID REFERENCES profiles(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX one_pending_application ON creator_applications(user_id) WHERE status = 'pending';

CREATE TABLE announcements (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title TEXT NOT NULL,
  body TEXT DEFAULT '',
  created_by UUID REFERENCES profiles(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE featured_rooms (
  room_id UUID PRIMARY KEY REFERENCES live_rooms(id) ON DELETE CASCADE,
  featured_by UUID REFERENCES profiles(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE platform_settings (
  key TEXT PRIMARY KEY,
  value BOOLEAN NOT NULL DEFAULT false
);
INSERT INTO platform_settings (key, value) VALUES ('gifting_paused', false), ('streams_paused', false);

CREATE TABLE withdrawal_approvals (
  request_id UUID NOT NULL REFERENCES withdrawal_requests(id) ON DELETE CASCADE,
  admin_id UUID NOT NULL REFERENCES profiles(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (request_id, admin_id)
);

CREATE TABLE fraud_alerts (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  reason TEXT NOT NULL,
  severity TEXT NOT NULL DEFAULT 'medium',
  resolved BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Reports: escalation + assignment
ALTER TABLE reports
  ADD COLUMN escalated BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN escalation_note TEXT,
  ADD COLUMN assigned_to UUID REFERENCES profiles(id);

-- Purchases need a refund state
ALTER TYPE transaction_status ADD VALUE IF NOT EXISTS 'refunded';

-- ----------------------------------------------------------------------------
-- 3. ROW LEVEL SECURITY
-- ----------------------------------------------------------------------------
ALTER TABLE moderation_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE strikes ENABLE ROW LEVEL SECURITY;
ALTER TABLE appeals ENABLE ROW LEVEL SECURITY;
ALTER TABLE banned_keywords ENABLE ROW LEVEL SECURITY;
ALTER TABLE held_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE creator_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE featured_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE platform_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE withdrawal_approvals ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE stream_categories ENABLE ROW LEVEL SECURITY;

-- All writes to the tables below go through the security-definer functions in section 5.
CREATE POLICY "Staff read tasks" ON moderation_tasks FOR SELECT USING (public.is_admin() OR assigned_to = auth.uid());
CREATE POLICY "Staff read strikes" ON strikes FOR SELECT USING (public.is_staff() OR user_id = auth.uid());
CREATE POLICY "Users read own appeals, admins read all" ON appeals FOR SELECT USING (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "Users file own appeal" ON appeals FOR INSERT WITH CHECK (user_id = auth.uid());
CREATE POLICY "Staff read keywords" ON banned_keywords FOR SELECT USING (public.is_staff());
CREATE POLICY "Staff read held messages" ON held_messages FOR SELECT USING (public.is_staff());
CREATE POLICY "Users read own application, admins all" ON creator_applications FOR SELECT USING (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "Users apply" ON creator_applications FOR INSERT WITH CHECK (user_id = auth.uid());
CREATE POLICY "Everyone reads announcements" ON announcements FOR SELECT USING (true);
CREATE POLICY "Everyone reads featured" ON featured_rooms FOR SELECT USING (true);
CREATE POLICY "Everyone reads settings" ON platform_settings FOR SELECT USING (true);
CREATE POLICY "Admins read approvals" ON withdrawal_approvals FOR SELECT USING (public.is_admin());
CREATE POLICY "Admins read fraud alerts" ON fraud_alerts FOR SELECT USING (public.is_admin());
CREATE POLICY "Admins read audit log" ON audit_logs FOR SELECT USING (public.is_admin());
CREATE POLICY "Users manage own devices" ON user_devices FOR ALL USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY "Categories readable" ON stream_categories FOR SELECT USING (true);

-- Policies that were missing in the first migration (RLS was on, no policies = locked)
CREATE POLICY "Read own follows" ON follows FOR SELECT USING (follower_id = auth.uid() OR following_id = auth.uid());
CREATE POLICY "Follow as self" ON follows FOR INSERT WITH CHECK (follower_id = auth.uid());
CREATE POLICY "Unfollow as self" ON follows FOR DELETE USING (follower_id = auth.uid());
CREATE POLICY "Read own blocks" ON blocks FOR SELECT USING (blocker_id = auth.uid());
CREATE POLICY "Block as self" ON blocks FOR INSERT WITH CHECK (blocker_id = auth.uid());
CREATE POLICY "Unblock as self" ON blocks FOR DELETE USING (blocker_id = auth.uid());
CREATE POLICY "Read own notifications" ON notifications FOR SELECT USING (user_id = auth.uid());
CREATE POLICY "Mark own notifications read" ON notifications FOR UPDATE USING (user_id = auth.uid());
CREATE POLICY "Viewers read presence" ON live_room_viewers FOR SELECT USING (true);
CREATE POLICY "Join as self" ON live_room_viewers FOR INSERT WITH CHECK (user_id = auth.uid());
CREATE POLICY "Leave as self" ON live_room_viewers FOR DELETE USING (user_id = auth.uid());
CREATE POLICY "Read own transactions" ON coin_transactions FOR SELECT USING (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "Read own gifts" ON gift_transactions FOR SELECT USING (sender_id = auth.uid() OR recipient_id = auth.uid() OR public.is_admin());
CREATE POLICY "Read own withdrawals" ON withdrawal_requests FOR SELECT USING (user_id = auth.uid() OR public.is_admin());
CREATE POLICY "Admins read all wallets" ON coin_wallets FOR SELECT USING (public.is_admin());
CREATE POLICY "Staff update reports" ON reports FOR UPDATE USING (public.is_staff());
CREATE POLICY "Staff delete chat" ON chat_messages FOR UPDATE USING (public.is_staff());

-- Chat: only active accounts may post
DROP POLICY IF EXISTS "Authenticated users can post chat" ON chat_messages;
CREATE POLICY "Active users post chat" ON chat_messages FOR INSERT WITH CHECK (
  auth.uid() = sender_id
  AND EXISTS (SELECT 1 FROM profiles p WHERE p.id = auth.uid() AND p.status = 'active')
);

-- Live rooms: only approved broadcasters/staff may go live, never during the kill switch
DROP POLICY IF EXISTS "Broadcasters manage own rooms" ON live_rooms;
CREATE POLICY "Approved creators start rooms" ON live_rooms FOR INSERT WITH CHECK (
  auth.uid() = host_id
  AND EXISTS (SELECT 1 FROM profiles p WHERE p.id = auth.uid()
              AND p.role IN ('broadcaster', 'moderator', 'admin') AND p.status = 'active')
  AND NOT COALESCE((SELECT value FROM platform_settings WHERE key = 'streams_paused'), false)
);
CREATE POLICY "Hosts update own rooms" ON live_rooms FOR UPDATE USING (auth.uid() = host_id);
CREATE POLICY "Hosts delete own rooms" ON live_rooms FOR DELETE USING (auth.uid() = host_id);

-- Profiles: reports/staff can see everything already (public read). Update policy stays;
-- protect_profile_fields() blocks role/status edits.

-- ----------------------------------------------------------------------------
-- 4. KEYWORD FILTER (holds the message for moderators instead of posting it)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.hold_flagged_chat() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  v_word text;
BEGIN
  IF public.is_staff() OR current_setting('viora.skip_hold', true) = '1' THEN RETURN NEW; END IF;
  SELECT word INTO v_word FROM banned_keywords WHERE lower(NEW.content) LIKE '%' || lower(word) || '%' LIMIT 1;
  IF v_word IS NOT NULL THEN
    INSERT INTO held_messages (room_id, sender_id, content, matched_keyword)
    VALUES (NEW.room_id, NEW.sender_id, NEW.content, v_word);
    RETURN NULL; -- cancels the insert
  END IF;
  RETURN NEW;
END; $$;

CREATE TRIGGER hold_flagged_chat BEFORE INSERT ON chat_messages
FOR EACH ROW EXECUTE FUNCTION public.hold_flagged_chat();

-- ----------------------------------------------------------------------------
-- 5. FUNCTIONS THE APP CALLS (supabase.rpc). Every one checks the caller's role.
-- ----------------------------------------------------------------------------

-- Roles (admin only)
CREATE OR REPLACE FUNCTION public.set_user_role(p_user uuid, p_role app_role) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  UPDATE profiles SET role = p_role WHERE id = p_user;
  PERFORM public.log_audit('set_role', 'profile', p_user::text, jsonb_build_object('role', p_role));
END; $$;

-- Tasks
CREATE OR REPLACE FUNCTION public.assign_task(p_title text, p_details text, p_report uuid, p_moderator uuid, p_priority task_priority)
RETURNS uuid LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE v_id uuid;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  IF NOT EXISTS (SELECT 1 FROM profiles WHERE id = p_moderator AND role = 'moderator') THEN
    RAISE EXCEPTION 'Assignee must be a moderator';
  END IF;
  INSERT INTO moderation_tasks (title, details, report_id, assigned_to, assigned_by, priority)
  VALUES (p_title, p_details, p_report, p_moderator, auth.uid(), p_priority) RETURNING id INTO v_id;
  IF p_report IS NOT NULL THEN UPDATE reports SET assigned_to = p_moderator WHERE id = p_report; END IF;
  PERFORM public.log_audit('assign_task', 'task', v_id::text);
  RETURN v_id;
END; $$;

CREATE OR REPLACE FUNCTION public.update_task(p_task uuid, p_status task_status, p_note text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT (public.is_admin() OR EXISTS (SELECT 1 FROM moderation_tasks WHERE id = p_task AND assigned_to = auth.uid())) THEN
    RAISE EXCEPTION 'Not your task';
  END IF;
  UPDATE moderation_tasks SET status = p_status, note = COALESCE(NULLIF(p_note, ''), note), updated_at = NOW() WHERE id = p_task;
  PERFORM public.log_audit('update_task', 'task', p_task::text, jsonb_build_object('status', p_status));
END; $$;

-- Strike ladder: 1 warning, 2 = 24h mute, 3 = 7d suspension, 4+ = ban
CREATE OR REPLACE FUNCTION public.apply_strike(p_user uuid, p_reason text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE v_n int;
BEGIN
  IF NOT public.is_staff() THEN RAISE EXCEPTION 'Staff only'; END IF;
  INSERT INTO strikes (user_id, issued_by, reason) VALUES (p_user, auth.uid(), p_reason);
  UPDATE profiles SET strikes = strikes + 1 WHERE id = p_user RETURNING strikes INTO v_n;
  UPDATE profiles SET
    status = CASE WHEN v_n >= 4 THEN 'banned'::account_status WHEN v_n = 3 THEN 'suspended'::account_status
                  WHEN v_n = 2 THEN 'muted'::account_status ELSE status END,
    status_until = CASE WHEN v_n = 3 THEN NOW() + interval '7 days' WHEN v_n = 2 THEN NOW() + interval '24 hours' ELSE status_until END
  WHERE id = p_user;
  PERFORM public.log_audit('strike', 'profile', p_user::text, jsonb_build_object('reason', p_reason, 'strikes', v_n));
END; $$;

-- Mutes and suspensions expire. Schedule with pg_cron every 10 minutes:
--   select cron.schedule('expire-status', '*/10 * * * *', $$ select public.expire_statuses() $$);
CREATE OR REPLACE FUNCTION public.expire_statuses() RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
  UPDATE profiles SET status = 'active', status_until = NULL
  WHERE status IN ('muted', 'suspended') AND status_until IS NOT NULL AND status_until < NOW();
$$;

CREATE OR REPLACE FUNCTION public.set_account_status(p_user uuid, p_status account_status) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  UPDATE profiles SET status = p_status, status_until = NULL,
         strikes = CASE WHEN p_status = 'active' THEN 0 ELSE strikes END WHERE id = p_user;
  IF p_status = 'banned' THEN UPDATE live_rooms SET status = 'ended', ended_at = NOW() WHERE host_id = p_user AND status = 'live'; END IF;
  PERFORM public.log_audit('set_status', 'profile', p_user::text, jsonb_build_object('status', p_status));
END; $$;

CREATE OR REPLACE FUNCTION public.escalate_report(p_report uuid, p_note text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_staff() THEN RAISE EXCEPTION 'Staff only'; END IF;
  UPDATE reports SET escalated = true, escalation_note = p_note WHERE id = p_report;
  PERFORM public.log_audit('escalate_report', 'report', p_report::text);
END; $$;

CREATE OR REPLACE FUNCTION public.resolve_report(p_report uuid, p_action text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_staff() THEN RAISE EXCEPTION 'Staff only'; END IF;
  UPDATE reports SET status = CASE WHEN p_action = 'DISMISS' THEN 'dismissed'::report_status ELSE 'action_taken'::report_status END,
                     action_taken = p_action, resolved_by = auth.uid(), resolved_at = NOW()
  WHERE id = p_report;
  PERFORM public.log_audit('resolve_report', 'report', p_report::text, jsonb_build_object('action', p_action));
END; $$;

-- Admin ends a room (kill room)
CREATE OR REPLACE FUNCTION public.kill_room(p_room uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  UPDATE live_rooms SET status = 'ended', ended_at = NOW() WHERE id = p_room;
  PERFORM public.log_audit('kill_room', 'room', p_room::text);
END; $$;

-- Held chat messages: staff release (posts it) or reject (+ strike)
CREATE OR REPLACE FUNCTION public.decide_held_message(p_id uuid, p_release boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE h held_messages;
BEGIN
  IF NOT public.is_staff() THEN RAISE EXCEPTION 'Staff only'; END IF;
  SELECT * INTO h FROM held_messages WHERE id = p_id;
  IF NOT FOUND THEN RETURN; END IF;
  IF p_release THEN
    PERFORM set_config('viora.skip_hold', '1', true);
    INSERT INTO chat_messages (room_id, sender_id, content) VALUES (h.room_id, h.sender_id, h.content);
    PERFORM set_config('viora.skip_hold', '0', true);
  ELSE
    PERFORM public.apply_strike(h.sender_id, 'Blocked keyword: ' || h.matched_keyword);
  END IF;
  DELETE FROM held_messages WHERE id = p_id;
  PERFORM public.log_audit(CASE WHEN p_release THEN 'release_held' ELSE 'reject_held' END, 'held_message', p_id::text);
END; $$;

-- Keywords
CREATE OR REPLACE FUNCTION public.set_keyword(p_word text, p_add boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  IF p_add THEN INSERT INTO banned_keywords (word, added_by) VALUES (lower(trim(p_word)), auth.uid()) ON CONFLICT DO NOTHING;
  ELSE DELETE FROM banned_keywords WHERE word = lower(trim(p_word)); END IF;
  PERFORM public.log_audit('keyword', 'keyword', p_word, jsonb_build_object('added', p_add));
END; $$;

-- Appeals
CREATE OR REPLACE FUNCTION public.decide_appeal(p_appeal uuid, p_approve boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE a appeals;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  SELECT * INTO a FROM appeals WHERE id = p_appeal AND status = 'pending';
  IF NOT FOUND THEN RAISE EXCEPTION 'Appeal not pending'; END IF;
  UPDATE appeals SET status = CASE WHEN p_approve THEN 'approved'::appeal_status ELSE 'rejected'::appeal_status END, decided_by = auth.uid() WHERE id = p_appeal;
  IF p_approve THEN PERFORM public.set_account_status(a.user_id, 'active'); END IF;
  PERFORM public.log_audit('decide_appeal', 'appeal', p_appeal::text, jsonb_build_object('approved', p_approve));
END; $$;

-- Creator applications
CREATE OR REPLACE FUNCTION public.decide_application(p_app uuid, p_approve boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE a creator_applications;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  SELECT * INTO a FROM creator_applications WHERE id = p_app AND status = 'pending';
  IF NOT FOUND THEN RAISE EXCEPTION 'Application not pending'; END IF;
  UPDATE creator_applications SET status = CASE WHEN p_approve THEN 'approved'::application_status ELSE 'rejected'::application_status END, decided_by = auth.uid() WHERE id = p_app;
  IF p_approve THEN UPDATE profiles SET role = 'broadcaster', is_verified = true WHERE id = a.user_id AND role = 'viewer'; END IF;
  PERFORM public.log_audit('decide_application', 'application', p_app::text, jsonb_build_object('approved', p_approve));
END; $$;

-- Announcements, featured rooms
CREATE OR REPLACE FUNCTION public.post_announcement(p_title text, p_body text) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  INSERT INTO announcements (title, body, created_by) VALUES (p_title, p_body, auth.uid());
  PERFORM public.log_audit('announcement', 'announcement', p_title);
END; $$;

CREATE OR REPLACE FUNCTION public.toggle_featured(p_room uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  IF EXISTS (SELECT 1 FROM featured_rooms WHERE room_id = p_room) THEN DELETE FROM featured_rooms WHERE room_id = p_room;
  ELSE INSERT INTO featured_rooms (room_id, featured_by) VALUES (p_room, auth.uid()); END IF;
  PERFORM public.log_audit('toggle_featured', 'room', p_room::text);
END; $$;

-- Kill switch
CREATE OR REPLACE FUNCTION public.set_platform_setting(p_key text, p_value boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  IF p_key NOT IN ('gifting_paused', 'streams_paused') THEN RAISE EXCEPTION 'Unknown setting'; END IF;
  UPDATE platform_settings SET value = p_value WHERE key = p_key;
  IF p_key = 'streams_paused' AND p_value THEN UPDATE live_rooms SET status = 'ended', ended_at = NOW() WHERE status = 'live'; END IF;
  PERFORM public.log_audit('kill_switch', 'setting', p_key, jsonb_build_object('value', p_value));
END; $$;

-- Account deletion (Google Play requirement). Cascades to profile, wallet, content.
CREATE OR REPLACE FUNCTION public.delete_my_account() RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, auth AS $$
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Not signed in'; END IF;
  PERFORM public.log_audit('delete_account', 'profile', auth.uid()::text);
  DELETE FROM auth.users WHERE id = auth.uid();
END; $$;

-- ----------------------------------------------------------------------------
-- 6. MONEY: ATOMIC GIFTS, SAFE WITHDRAWALS, REFUNDS, FRAUD SCAN
-- ----------------------------------------------------------------------------

-- Atomic gift. Replaces the read-then-write logic in the edge function.
CREATE OR REPLACE FUNCTION public.send_gift(p_room uuid, p_gift uuid, p_combo int, p_key text) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  v_room live_rooms; v_gift gift_catalog; v_total bigint; v_diamonds bigint; v_tx uuid; v_existing uuid;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Not signed in'; END IF;
  IF p_combo IS NULL OR p_combo < 1 OR p_combo > 999 THEN RAISE EXCEPTION 'Invalid quantity'; END IF;
  IF COALESCE((SELECT value FROM platform_settings WHERE key = 'gifting_paused'), false) THEN
    RAISE EXCEPTION 'Gifting is temporarily paused';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND status = 'active') THEN
    RAISE EXCEPTION 'Account restricted';
  END IF;

  SELECT id INTO v_existing FROM gift_transactions WHERE idempotency_key = p_key;
  IF FOUND THEN RETURN jsonb_build_object('success', true, 'transactionId', v_existing, 'duplicate', true); END IF;

  SELECT * INTO v_room FROM live_rooms WHERE id = p_room AND status = 'live';
  IF NOT FOUND THEN RAISE EXCEPTION 'Live room not available'; END IF;
  IF v_room.host_id = auth.uid() THEN RAISE EXCEPTION 'You cannot gift yourself'; END IF;

  SELECT * INTO v_gift FROM gift_catalog WHERE id = p_gift AND is_active;
  IF NOT FOUND THEN RAISE EXCEPTION 'Invalid gift'; END IF;

  v_total := v_gift.coin_cost::bigint * p_combo;
  v_diamonds := floor(v_total * 0.70);

  -- Row lock + conditional update = no double spend
  UPDATE coin_wallets SET coin_balance = coin_balance - v_total, updated_at = NOW()
  WHERE user_id = auth.uid() AND coin_balance >= v_total;
  IF NOT FOUND THEN RAISE EXCEPTION 'Insufficient coin balance'; END IF;

  UPDATE coin_wallets SET diamond_balance = diamond_balance + v_diamonds, updated_at = NOW() WHERE user_id = v_room.host_id;

  INSERT INTO gift_transactions (room_id, sender_id, recipient_id, gift_id, combo_count, total_coins, diamonds_awarded, idempotency_key)
  VALUES (p_room, auth.uid(), v_room.host_id, p_gift, p_combo, v_total, v_diamonds, p_key) RETURNING id INTO v_tx;

  INSERT INTO coin_transactions (user_id, type, amount_coins, reference_id) VALUES (auth.uid(), 'gift_sent', -v_total, v_tx::text);
  INSERT INTO coin_transactions (user_id, type, amount_diamonds, reference_id) VALUES (v_room.host_id, 'gift_received', v_diamonds, v_tx::text);

  UPDATE live_rooms SET gift_count = gift_count + p_combo, diamond_count = diamond_count + v_diamonds WHERE id = p_room;

  RETURN jsonb_build_object('success', true, 'transactionId', v_tx, 'totalCoins', v_total, 'diamondsAwarded', v_diamonds);
END; $$;

-- Withdrawal request with safety rules: minimum 500 diamonds ($5)
CREATE OR REPLACE FUNCTION public.request_withdrawal(p_diamonds bigint, p_method text, p_destination text) RETURNS uuid
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE v_id uuid;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Not signed in'; END IF;
  IF p_diamonds < 500 THEN RAISE EXCEPTION 'Minimum cash out is 500 diamonds ($5.00)'; END IF;
  IF NOT EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND status = 'active') THEN RAISE EXCEPTION 'Account restricted'; END IF;

  UPDATE coin_wallets SET diamond_balance = diamond_balance - p_diamonds, updated_at = NOW()
  WHERE user_id = auth.uid() AND diamond_balance >= p_diamonds;
  IF NOT FOUND THEN RAISE EXCEPTION 'Insufficient diamonds'; END IF;

  INSERT INTO withdrawal_requests (user_id, diamonds_amount, usd_amount, payout_method, payout_destination)
  VALUES (auth.uid(), p_diamonds, p_diamonds / 100.0, p_method, p_destination) RETURNING id INTO v_id;
  INSERT INTO coin_transactions (user_id, type, amount_diamonds, status, reference_id)
  VALUES (auth.uid(), 'withdrawal', -p_diamonds, 'pending', v_id::text);
  RETURN v_id;
END; $$;

-- Approve: new creators (account < 7 days) are held; 50,000+ diamonds ($500) need TWO different admins
CREATE OR REPLACE FUNCTION public.approve_withdrawal(p_request uuid) RETURNS text
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE w withdrawal_requests; v_created timestamptz; v_needed int; v_have int;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  SELECT * INTO w FROM withdrawal_requests WHERE id = p_request AND status = 'pending' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Request not pending'; END IF;
  SELECT created_at INTO v_created FROM profiles WHERE id = w.user_id;
  IF v_created > NOW() - interval '7 days' THEN RAISE EXCEPTION 'New-creator security hold: account is under 7 days old'; END IF;

  INSERT INTO withdrawal_approvals (request_id, admin_id) VALUES (p_request, auth.uid()) ON CONFLICT DO NOTHING;
  v_needed := CASE WHEN w.diamonds_amount >= 50000 THEN 2 ELSE 1 END;
  SELECT count(*) INTO v_have FROM withdrawal_approvals WHERE request_id = p_request;

  IF v_have >= v_needed THEN
    UPDATE withdrawal_requests SET status = 'completed', updated_at = NOW() WHERE id = p_request;
    UPDATE coin_transactions SET status = 'completed' WHERE type = 'withdrawal' AND reference_id = p_request::text;
    PERFORM public.log_audit('approve_withdrawal', 'withdrawal', p_request::text);
    RETURN 'approved';
  END IF;
  PERFORM public.log_audit('first_approval_withdrawal', 'withdrawal', p_request::text);
  RETURN 'waiting_for_second_admin';
END; $$;

CREATE OR REPLACE FUNCTION public.reject_withdrawal(p_request uuid, p_notes text DEFAULT '') RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE w withdrawal_requests;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  SELECT * INTO w FROM withdrawal_requests WHERE id = p_request AND status = 'pending' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Request not pending'; END IF;
  UPDATE withdrawal_requests SET status = 'rejected', admin_notes = p_notes, updated_at = NOW() WHERE id = p_request;
  UPDATE coin_wallets SET diamond_balance = diamond_balance + w.diamonds_amount WHERE user_id = w.user_id; -- give the diamonds back
  UPDATE coin_transactions SET status = 'rejected' WHERE type = 'withdrawal' AND reference_id = p_request::text;
  PERFORM public.log_audit('reject_withdrawal', 'withdrawal', p_request::text);
END; $$;

-- Refund a coin purchase (admin). Only if the buyer still has the coins.
CREATE OR REPLACE FUNCTION public.refund_purchase(p_tx uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE t coin_transactions;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  SELECT * INTO t FROM coin_transactions WHERE id = p_tx AND type = 'coin_purchase' AND status = 'completed' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Purchase not refundable'; END IF;
  UPDATE coin_wallets SET coin_balance = coin_balance - t.amount_coins WHERE user_id = t.user_id AND coin_balance >= t.amount_coins;
  IF NOT FOUND THEN RAISE EXCEPTION 'Buyer already spent these coins. Flag as fraud instead.'; END IF;
  UPDATE coin_transactions SET status = 'refunded' WHERE id = p_tx;
  PERFORM public.log_audit('refund_purchase', 'purchase', p_tx::text);
END; $$;
-- NOTE: the actual money refund is done in Google Play Console. This only fixes the in-app balance.

-- Fraud scan: run every 10 minutes with pg_cron, or call from an Edge Function.
--   select cron.schedule('fraud-scan', '*/10 * * * *', $$ select public.scan_fraud() $$);
CREATE OR REPLACE FUNCTION public.scan_fraud() RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  -- Rule 1: account under 1 day old bought 10,000+ coins
  INSERT INTO fraud_alerts (user_id, reason, severity)
  SELECT t.user_id, 'New account bought ' || sum(t.amount_coins) || ' coins in its first day', 'high'
  FROM coin_transactions t JOIN profiles p ON p.id = t.user_id
  WHERE t.type = 'coin_purchase' AND p.created_at > NOW() - interval '1 day'
  GROUP BY t.user_id HAVING sum(t.amount_coins) >= 10000
  AND NOT EXISTS (SELECT 1 FROM fraud_alerts f WHERE f.user_id = t.user_id AND NOT f.resolved);

  -- Rule 2: 5+ gifts in 24h and every one went to the same host
  INSERT INTO fraud_alerts (user_id, reason, severity)
  SELECT g.sender_id, 'All ' || count(*) || ' gifts in 24h went to one host', 'medium'
  FROM gift_transactions g
  WHERE g.created_at > NOW() - interval '24 hours'
  GROUP BY g.sender_id HAVING count(*) >= 5 AND count(DISTINCT g.recipient_id) = 1
  AND NOT EXISTS (SELECT 1 FROM fraud_alerts f WHERE f.user_id = g.sender_id AND NOT f.resolved);
END; $$;

CREATE OR REPLACE FUNCTION public.resolve_fraud_alert(p_alert uuid, p_ban boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE v_user uuid;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  UPDATE fraud_alerts SET resolved = true WHERE id = p_alert RETURNING user_id INTO v_user;
  IF p_ban THEN PERFORM public.set_account_status(v_user, 'banned'); END IF;
  PERFORM public.log_audit('resolve_fraud', 'fraud_alert', p_alert::text, jsonb_build_object('ban', p_ban));
END; $$;

-- Daily revenue report (admin)
CREATE OR REPLACE FUNCTION public.revenue_report(p_days int DEFAULT 1) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  RETURN jsonb_build_object(
    'coins_sold', (SELECT COALESCE(sum(amount_coins), 0) FROM coin_transactions WHERE type = 'coin_purchase' AND status = 'completed' AND created_at > NOW() - make_interval(days => p_days)),
    'gross_usd', (SELECT COALESCE(sum(amount_usd), 0) FROM coin_transactions WHERE type = 'coin_purchase' AND status = 'completed' AND created_at > NOW() - make_interval(days => p_days)),
    'diamonds_earned', (SELECT COALESCE(sum(diamonds_awarded), 0) FROM gift_transactions WHERE created_at > NOW() - make_interval(days => p_days)),
    'payouts_owed_usd', (SELECT COALESCE(sum(usd_amount), 0) FROM withdrawal_requests WHERE status = 'pending'),
    'payouts_paid_usd', (SELECT COALESCE(sum(usd_amount), 0) FROM withdrawal_requests WHERE status = 'completed' AND updated_at > NOW() - make_interval(days => p_days))
  );
END; $$;

-- ----------------------------------------------------------------------------
-- 7. LOCK DOWN DIRECT TABLE WRITES FROM THE APP
-- ----------------------------------------------------------------------------
REVOKE INSERT, UPDATE, DELETE ON coin_wallets, coin_transactions, gift_transactions, withdrawal_requests,
  moderation_tasks, strikes, banned_keywords, held_messages, announcements, featured_rooms,
  platform_settings, withdrawal_approvals, fraud_alerts, audit_logs FROM anon, authenticated;

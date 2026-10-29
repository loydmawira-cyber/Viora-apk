-- ============================================================================
-- VIORA FOLLOW-UPS: catalog editing, underage routing, assigned report queue,
-- moderator performance. Run AFTER 20261029000000_roles_and_safety.sql.
-- Written without a live database to test against: run it on a fresh Supabase
-- project first and fix any error it reports.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. COIN PACKAGES (editable by admins) + GIFT EDITING
-- ----------------------------------------------------------------------------
CREATE TABLE coin_packages (
  id TEXT PRIMARY KEY,
  coins BIGINT NOT NULL CHECK (coins > 0),
  bonus_coins BIGINT NOT NULL DEFAULT 0 CHECK (bonus_coins >= 0),
  price_usd NUMERIC(10, 2) NOT NULL CHECK (price_usd > 0),
  play_product_id TEXT, -- must match the in-app product created in Google Play Console
  is_active BOOLEAN NOT NULL DEFAULT true
);
INSERT INTO coin_packages (id, coins, bonus_coins, price_usd) VALUES
  ('pkg_100', 100, 0, 0.99), ('pkg_500', 500, 50, 4.99), ('pkg_1200', 1200, 150, 9.99),
  ('pkg_3000', 3000, 500, 24.99), ('pkg_6500', 6500, 1200, 49.99), ('pkg_15000', 15000, 3500, 99.99);

ALTER TABLE coin_packages ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Everyone reads packages" ON coin_packages FOR SELECT USING (true);
REVOKE INSERT, UPDATE, DELETE ON coin_packages FROM anon, authenticated;

CREATE OR REPLACE FUNCTION public.update_coin_package(p_id text, p_coins bigint, p_bonus bigint, p_price numeric) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  UPDATE coin_packages SET coins = p_coins, bonus_coins = p_bonus, price_usd = p_price WHERE id = p_id;
  PERFORM public.log_audit('edit_coin_package', 'coin_package', p_id, jsonb_build_object('coins', p_coins, 'bonus', p_bonus, 'price', p_price));
END; $$;

CREATE OR REPLACE FUNCTION public.set_coin_package_active(p_id text, p_active boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  UPDATE coin_packages SET is_active = p_active WHERE id = p_id;
  PERFORM public.log_audit('toggle_coin_package', 'coin_package', p_id, jsonb_build_object('active', p_active));
END; $$;

CREATE OR REPLACE FUNCTION public.update_gift(p_id uuid, p_cost int, p_active boolean) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  IF p_cost <= 0 THEN RAISE EXCEPTION 'Cost must be positive'; END IF;
  UPDATE gift_catalog SET coin_cost = p_cost, is_active = p_active WHERE id = p_id;
  PERFORM public.log_audit('edit_gift', 'gift', p_id::text, jsonb_build_object('cost', p_cost, 'active', p_active));
END; $$;

-- ----------------------------------------------------------------------------
-- 2. UNDERAGE REPORTS GO STRAIGHT TO ADMINS
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.route_underage_report() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF upper(NEW.category) = 'UNDERAGE' THEN
    NEW.escalated := true;
    NEW.escalation_note := 'Suspected minor: sent straight to admins';
  END IF;
  RETURN NEW;
END; $$;

CREATE TRIGGER route_underage_report BEFORE INSERT ON reports
FOR EACH ROW EXECUTE FUNCTION public.route_underage_report();

-- Moderators only see reports that are unassigned or assigned to them, never underage ones.
-- Admins see everything. (Replaces the broader policies from earlier migrations.)
DROP POLICY IF EXISTS "Moderators can view reports" ON reports;
DROP POLICY IF EXISTS "Staff update reports" ON reports;

CREATE POLICY "Admins view all reports" ON reports FOR SELECT USING (public.is_admin());
CREATE POLICY "Moderators view their queue" ON reports FOR SELECT USING (
  public.is_staff()
  AND upper(category) <> 'UNDERAGE'
  AND (assigned_to IS NULL OR assigned_to = auth.uid())
);
CREATE POLICY "Reporters view own reports" ON reports FOR SELECT USING (reporter_id = auth.uid());
-- All report changes go through resolve_report / escalate_report / claim_report (security definer).

-- A moderator takes an unassigned report
CREATE OR REPLACE FUNCTION public.claim_report(p_report uuid) RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_staff() THEN RAISE EXCEPTION 'Staff only'; END IF;
  UPDATE reports SET assigned_to = auth.uid()
  WHERE id = p_report AND assigned_to IS NULL AND (upper(category) <> 'UNDERAGE' OR public.is_admin());
  IF NOT FOUND THEN RAISE EXCEPTION 'Report is not available to claim'; END IF;
  PERFORM public.log_audit('claim_report', 'report', p_report::text);
END; $$;

-- ----------------------------------------------------------------------------
-- 3. MODERATOR PERFORMANCE (calculated from real work, admin only)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_moderator_performance()
RETURNS TABLE (moderator_id uuid, name text, reports_cleared bigint, avg_minutes numeric, tasks_done bigint, tasks_open bigint)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'Admins only'; END IF;
  RETURN QUERY
  SELECT p.id, p.display_name,
         (SELECT count(*) FROM reports r WHERE r.resolved_by = p.id),
         (SELECT round(avg(extract(epoch FROM (r.resolved_at - r.created_at)) / 60)::numeric, 1)
            FROM reports r WHERE r.resolved_by = p.id AND r.resolved_at IS NOT NULL),
         (SELECT count(*) FROM moderation_tasks t WHERE t.assigned_to = p.id AND t.status = 'done'),
         (SELECT count(*) FROM moderation_tasks t WHERE t.assigned_to = p.id AND t.status <> 'done')
  FROM profiles p WHERE p.role = 'moderator' ORDER BY p.display_name;
END; $$;

-- ----------------------------------------------------------------------------
-- 4. NOTES FOR THE APP (no SQL needed)
-- * Silent moderator view: when staff open a reported stream, the app must NOT insert a row
--   into live_room_viewers and must NOT call anything that raises viewer_count.
-- * Daily revenue report: call revenue_report(1) for today, revenue_report(7) for the week.
-- * Push for announcements: deploy the send-announcement edge function (see supabase/functions).
-- ============================================================================

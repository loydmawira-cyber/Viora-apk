-- ============================================================================
-- VIORA PRODUCTION DATABASE SCHEMA
-- PostgreSQL with Row Level Security (RLS), Triggers, & Audit Logging
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ----------------------------------------------------------------------------
-- 1. USER ROLES ENUM & TABLE
-- ----------------------------------------------------------------------------
CREATE TYPE app_role AS ENUM ('viewer', 'broadcaster', 'moderator', 'admin');
CREATE TYPE stream_status AS ENUM ('live', 'paused', 'ended');
CREATE TYPE transaction_type AS ENUM ('coin_purchase', 'gift_sent', 'gift_received', 'withdrawal');
CREATE TYPE transaction_status AS ENUM ('pending', 'completed', 'failed', 'rejected');
CREATE TYPE report_status AS ENUM ('pending_review', 'dismissed', 'action_taken');

-- ----------------------------------------------------------------------------
-- 2. PROFILES
-- ----------------------------------------------------------------------------
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL,
    display_name TEXT NOT NULL,
    avatar_url TEXT,
    bio TEXT DEFAULT '',
    role app_role DEFAULT 'viewer' NOT NULL,
    level INT DEFAULT 1 NOT NULL,
    is_verified BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ----------------------------------------------------------------------------
-- 3. STREAM CATEGORIES
-- ----------------------------------------------------------------------------
CREATE TABLE stream_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    icon_name TEXT NOT NULL,
    display_order INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ----------------------------------------------------------------------------
-- 4. LIVE ROOMS
-- ----------------------------------------------------------------------------
CREATE TABLE live_rooms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    host_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    category_id UUID REFERENCES stream_categories(id) ON DELETE SET NULL,
    agora_channel_name TEXT UNIQUE NOT NULL,
    status stream_status DEFAULT 'live' NOT NULL,
    thumbnail_url TEXT,
    viewer_count INT DEFAULT 0 NOT NULL,
    like_count BIGINT DEFAULT 0 NOT NULL,
    gift_count BIGINT DEFAULT 0 NOT NULL,
    diamond_count BIGINT DEFAULT 0 NOT NULL,
    pinned_comment TEXT,
    started_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    ended_at TIMESTAMPTZ,
    recording_url TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ----------------------------------------------------------------------------
-- 5. LIVE ROOM VIEWERS & MODERATORS
-- ----------------------------------------------------------------------------
CREATE TABLE live_room_viewers (
    room_id UUID NOT NULL REFERENCES live_rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    last_seen_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    PRIMARY KEY (room_id, user_id)
);

CREATE TABLE live_room_moderators (
    room_id UUID NOT NULL REFERENCES live_rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    assigned_by UUID NOT NULL REFERENCES profiles(id),
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    PRIMARY KEY (room_id, user_id)
);

-- ----------------------------------------------------------------------------
-- 6. SOCIAL GRAPH: FOLLOWS & BLOCKS
-- ----------------------------------------------------------------------------
CREATE TABLE follows (
    follower_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    PRIMARY KEY (follower_id, following_id),
    CHECK (follower_id <> following_id)
);

CREATE TABLE blocks (
    blocker_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);

-- ----------------------------------------------------------------------------
-- 7. CHAT MESSAGES & REACTIONS
-- ----------------------------------------------------------------------------
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    room_id UUID NOT NULL REFERENCES live_rooms(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_pinned BOOLEAN DEFAULT false NOT NULL,
    is_deleted BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

CREATE TABLE reactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    room_id UUID NOT NULL REFERENCES live_rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    reaction_type TEXT DEFAULT 'heart' NOT NULL,
    count INT DEFAULT 1 NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ----------------------------------------------------------------------------
-- 8. VIRTUAL GIFTS & WALLET LEDGER
-- ----------------------------------------------------------------------------
CREATE TABLE gift_catalog (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    emoji TEXT NOT NULL,
    coin_cost INT NOT NULL CHECK (coin_cost > 0),
    description TEXT DEFAULT '',
    animation_type TEXT DEFAULT 'POP' NOT NULL,
    is_active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

CREATE TABLE coin_wallets (
    user_id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
    coin_balance BIGINT DEFAULT 0 NOT NULL CHECK (coin_balance >= 0),
    diamond_balance BIGINT DEFAULT 0 NOT NULL CHECK (diamond_balance >= 0),
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

CREATE TABLE coin_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    type transaction_type NOT NULL,
    amount_coins BIGINT DEFAULT 0 NOT NULL,
    amount_diamonds BIGINT DEFAULT 0 NOT NULL,
    amount_usd NUMERIC(10, 2),
    status transaction_status DEFAULT 'completed' NOT NULL,
    idempotency_key TEXT UNIQUE,
    reference_id TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

CREATE TABLE gift_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    room_id UUID NOT NULL REFERENCES live_rooms(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    gift_id UUID NOT NULL REFERENCES gift_catalog(id),
    combo_count INT DEFAULT 1 NOT NULL CHECK (combo_count >= 1),
    total_coins BIGINT NOT NULL CHECK (total_coins > 0),
    diamonds_awarded BIGINT NOT NULL CHECK (diamonds_awarded >= 0),
    idempotency_key TEXT UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

CREATE TABLE withdrawal_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    diamonds_amount BIGINT NOT NULL CHECK (diamonds_amount >= 100),
    usd_amount NUMERIC(10, 2) NOT NULL,
    payout_method TEXT NOT NULL,
    payout_destination TEXT NOT NULL,
    status transaction_status DEFAULT 'pending' NOT NULL,
    admin_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ----------------------------------------------------------------------------
-- 9. SAFETY & MODERATION REPORTS
-- ----------------------------------------------------------------------------
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    target_type TEXT NOT NULL, -- 'stream', 'user', 'chat'
    target_id TEXT NOT NULL,
    category TEXT NOT NULL,
    notes TEXT DEFAULT '',
    status report_status DEFAULT 'pending_review' NOT NULL,
    action_taken TEXT,
    resolved_by UUID REFERENCES profiles(id),
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    resolved_at TIMESTAMPTZ
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id UUID REFERENCES profiles(id) ON DELETE SET NULL,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    ip_address TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ----------------------------------------------------------------------------
-- 10. DEVICE TOKENS & NOTIFICATIONS
-- ----------------------------------------------------------------------------
CREATE TABLE user_devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    fcm_token TEXT NOT NULL,
    platform TEXT NOT NULL, -- 'android', 'ios'
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    UNIQUE(user_id, fcm_token)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    type TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    is_read BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE live_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE live_room_viewers ENABLE ROW LEVEL SECURITY;
ALTER TABLE follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE gift_catalog ENABLE ROW LEVEL SECURITY;
ALTER TABLE coin_wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE coin_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE gift_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE withdrawal_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Profiles: Public can read, user can update own profile
CREATE POLICY "Public profiles are viewable by everyone" ON profiles FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);

-- Live Rooms: Public can read active rooms, hosts can update own rooms
CREATE POLICY "Public live rooms viewable" ON live_rooms FOR SELECT USING (true);
CREATE POLICY "Broadcasters manage own rooms" ON live_rooms FOR ALL USING (auth.uid() = host_id);

-- Chat Messages: Everyone can view, authenticated users can insert, sender or mod can delete
CREATE POLICY "Chat messages are viewable by all" ON chat_messages FOR SELECT USING (true);
CREATE POLICY "Authenticated users can post chat" ON chat_messages FOR INSERT WITH CHECK (auth.uid() = sender_id);

-- Wallets: Users can view only their own wallet; balance edits ONLY by server functions
CREATE POLICY "Users view own wallet" ON coin_wallets FOR SELECT USING (auth.uid() = user_id);

-- Gift Catalog: Viewable by all
CREATE POLICY "Gift catalog viewable by all" ON gift_catalog FOR SELECT USING (true);

-- Reports: Users can insert, only mods/admins can view
CREATE POLICY "Users can submit reports" ON reports FOR INSERT WITH CHECK (auth.uid() = reporter_id);
CREATE POLICY "Moderators can view reports" ON reports FOR SELECT USING (
    EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND profiles.role IN ('moderator', 'admin'))
);

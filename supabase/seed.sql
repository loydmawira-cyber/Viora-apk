-- ============================================================================
-- VIORA SEED DATA
-- Default Stream Categories & Virtual Gift Catalog
-- ============================================================================

INSERT INTO stream_categories (slug, name, icon_name, display_order) VALUES
('trending', 'Trending', 'Whatshot', 1),
('music', 'Music & Jam', 'MusicNote', 2),
('gaming', 'Gaming & Esports', 'SportsEsports', 3),
('talent', 'Talent & Dance', 'Star', 4),
('chatting', 'Just Chatting', 'Chat', 5),
('fitness', 'Fitness & Health', 'FitnessCenter', 6),
('creative', 'Art & Design', 'Brush', 7)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO gift_catalog (slug, name, emoji, coin_cost, description, animation_type) VALUES
('gift_rose', 'Rose', '🌹', 1, 'A lovely crimson rose', 'POP'),
('gift_coffee', 'Coffee', '☕', 5, 'Fuel the broadcast energy', 'POP'),
('gift_heart', 'Sparkle Heart', '💖', 10, 'Warm glowing appreciation', 'BOUNCE'),
('gift_fire', 'Fire Energy', '🔥', 25, 'The stream is on fire!', 'FLAME'),
('gift_ring', 'Diamond Ring', '💍', 50, 'A pledge of devotion', 'SHINE'),
('gift_car', 'Sports Car', '🏎️', 200, 'Cruising in luxury', 'DRIVE_BY'),
('gift_rocket', 'Galactic Rocket', '🚀', 500, 'Blast off to the stars', 'LAUNCH'),
('gift_crown', 'Viora Crown', '👑', 1000, 'Royalty of the live stream', 'CROWN_REVEAL')
ON CONFLICT (slug) DO NOTHING;

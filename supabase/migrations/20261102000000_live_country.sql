-- Each live room remembers the country it is broadcasting from (detected by the app).
ALTER TABLE live_rooms ADD COLUMN IF NOT EXISTS country_code TEXT NOT NULL DEFAULT 'ALL';

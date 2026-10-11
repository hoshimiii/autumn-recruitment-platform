ALTER TABLE jobs ADD COLUMN source_row_id TEXT;
ALTER TABLE jobs ADD COLUMN official_job_id TEXT;
CREATE TABLE browser_source_policies (
 source_id INTEGER PRIMARY KEY REFERENCES sources(id) ON DELETE CASCADE,
 source_version INTEGER NOT NULL, terms_version TEXT NOT NULL, approved_at TEXT NOT NULL,
 adapter TEXT NOT NULL CHECK(adapter IN ('fixture','sub2api')), revoked INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE browser_tasks (
 id INTEGER PRIMARY KEY AUTOINCREMENT, source_id INTEGER NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
 source_version INTEGER NOT NULL, terms_version TEXT NOT NULL, origin TEXT NOT NULL, list_url TEXT NOT NULL,
 row_start INTEGER NOT NULL CHECK(row_start>=0), row_end INTEGER NOT NULL CHECK(row_end>row_start),
 max_rows INTEGER NOT NULL, max_chars INTEGER NOT NULL, read_delay_seconds INTEGER NOT NULL, adapter TEXT NOT NULL,
 fixture_mode INTEGER NOT NULL DEFAULT 0 CHECK(fixture_mode IN (0,1)),
 status TEXT NOT NULL DEFAULT 'PENDING', worker_id TEXT, lease_token TEXT, lease_expires_at INTEGER,
 result_hash TEXT, error TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE browser_ingestion_evidence (
 id INTEGER PRIMARY KEY AUTOINCREMENT, task_id INTEGER NOT NULL REFERENCES browser_tasks(id) ON DELETE CASCADE,
 source_id INTEGER NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
 source_row_id TEXT NOT NULL, item_discriminator TEXT NOT NULL, content_hash TEXT NOT NULL,
 evidence_json TEXT NOT NULL, manual_item_id INTEGER REFERENCES manual_items(id) ON DELETE SET NULL,
 review_item_id INTEGER REFERENCES review_items(id) ON DELETE SET NULL,
 UNIQUE(source_id,source_row_id,item_discriminator,content_hash)
);
CREATE TABLE browser_source_pacing (
 source_id INTEGER PRIMARY KEY REFERENCES sources(id) ON DELETE CASCADE, next_read_at INTEGER NOT NULL
);
CREATE INDEX idx_browser_task_claim ON browser_tasks(status,lease_expires_at);

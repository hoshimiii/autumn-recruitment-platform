ALTER TABLE sources ADD COLUMN list_url TEXT;
ALTER TABLE sources ADD COLUMN cron TEXT NOT NULL DEFAULT '0 0 8 * * *';
ALTER TABLE sources ADD COLUMN timezone TEXT NOT NULL DEFAULT 'Asia/Shanghai';
ALTER TABLE sources ADD COLUMN min_interval_seconds INTEGER NOT NULL DEFAULT 5
  CHECK (min_interval_seconds >= 0);
ALTER TABLE sources ADD COLUMN max_pages INTEGER NOT NULL DEFAULT 1 CHECK (max_pages > 0);
ALTER TABLE sources ADD COLUMN max_items INTEGER NOT NULL DEFAULT 50 CHECK (max_items > 0);
ALTER TABLE sources ADD COLUMN selectors_json TEXT NOT NULL DEFAULT '{}';
ALTER TABLE sources ADD COLUMN field_mapping_json TEXT NOT NULL DEFAULT '{}';
ALTER TABLE sources ADD COLUMN terms_url TEXT;
ALTER TABLE sources ADD COLUMN terms_confirmed_at TEXT;
ALTER TABLE sources ADD COLUMN robots_url TEXT;
ALTER TABLE sources ADD COLUMN timeout_seconds INTEGER NOT NULL DEFAULT 15 CHECK (timeout_seconds > 0);
ALTER TABLE sources ADD COLUMN etag TEXT;
ALTER TABLE sources ADD COLUMN last_modified TEXT;
ALTER TABLE sources ADD COLUMN last_run_at TEXT;
ALTER TABLE sources ADD COLUMN last_status TEXT;
ALTER TABLE sources ADD COLUMN ai_enabled INTEGER NOT NULL DEFAULT 0 CHECK (ai_enabled IN (0, 1));
ALTER TABLE sources ADD COLUMN ai_provider TEXT;
ALTER TABLE sources ADD COLUMN ai_model TEXT;
ALTER TABLE review_items ADD COLUMN content_hash TEXT;

CREATE INDEX idx_sources_collection ON sources(enabled, compliance_status, mode);
CREATE INDEX idx_review_dedupe_hash ON review_items(source_id, dedupe_key, content_hash);
CREATE UNIQUE INDEX idx_review_unique_content
  ON review_items(source_id, dedupe_key, content_hash) WHERE content_hash IS NOT NULL;
CREATE UNIQUE INDEX idx_jobs_unique_dedupe
  ON jobs(dedupe_key) WHERE dedupe_key IS NOT NULL;
CREATE INDEX idx_runs_source_started ON collection_runs(source_id, started_at DESC);

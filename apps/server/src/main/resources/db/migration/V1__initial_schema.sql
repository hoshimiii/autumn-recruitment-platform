CREATE TABLE sources (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL, type TEXT NOT NULL, base_url TEXT NOT NULL, adapter_type TEXT NOT NULL,
  mode TEXT NOT NULL, compliance_status TEXT NOT NULL, trust_level TEXT NOT NULL, scope TEXT NOT NULL,
  enabled INTEGER NOT NULL DEFAULT 1 CHECK (enabled IN (0, 1)), secret_env_ref TEXT,
  config_version INTEGER NOT NULL DEFAULT 1, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE collection_runs (
  id INTEGER PRIMARY KEY AUTOINCREMENT, source_id INTEGER REFERENCES sources(id) ON DELETE SET NULL,
  trigger_type TEXT NOT NULL, status TEXT NOT NULL, started_at TEXT, finished_at TEXT,
  discovered_count INTEGER NOT NULL DEFAULT 0, review_item_count INTEGER NOT NULL DEFAULT 0,
  error_count INTEGER NOT NULL DEFAULT 0, error_summary TEXT
);
CREATE TABLE jobs (
  id INTEGER PRIMARY KEY AUTOINCREMENT, company_type TEXT, company_name TEXT NOT NULL, business_unit TEXT,
  title TEXT NOT NULL, normalized_role TEXT, job_family TEXT, job_type TEXT NOT NULL, campus_batch TEXT,
  graduation_window TEXT, city TEXT, country_region TEXT, remote_policy TEXT, start_date TEXT, deadline TEXT,
  status TEXT NOT NULL, priority INTEGER NOT NULL DEFAULT 0, tech_stack TEXT NOT NULL DEFAULT '[]',
  certificate_requirements TEXT NOT NULL DEFAULT '[]', qualification_notes TEXT, education_requirement TEXT,
  major_requirement TEXT, language_requirement TEXT, experience_requirement TEXT, salary_range TEXT, jd TEXT,
  requirements TEXT, source_id INTEGER REFERENCES sources(id) ON DELETE SET NULL, source_url TEXT,
  apply_url TEXT, apply_method TEXT, requires_login INTEGER NOT NULL DEFAULT 0, requires_captcha INTEGER NOT NULL DEFAULT 0,
  resume_format TEXT, custom_questions_schema TEXT, source_trust_level TEXT, dedupe_key TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE job_snapshots (
  id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  source_id INTEGER REFERENCES sources(id) ON DELETE SET NULL, source_url TEXT NOT NULL, content_hash TEXT,
  raw_content TEXT, parsed_payload TEXT, attachment_paths TEXT NOT NULL DEFAULT '[]',
  captured_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE review_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT, source_id INTEGER REFERENCES sources(id) ON DELETE SET NULL,
  run_id INTEGER REFERENCES collection_runs(id) ON DELETE SET NULL, candidate_payload TEXT NOT NULL,
  normalized_payload TEXT NOT NULL, raw_snapshot_id INTEGER, dedupe_key TEXT, confidence REAL, status TEXT NOT NULL,
  review_notes TEXT, published_job_id INTEGER REFERENCES jobs(id) ON DELETE SET NULL,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE audit_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT, entity_type TEXT NOT NULL, entity_id INTEGER NOT NULL, action TEXT NOT NULL,
  before_payload TEXT, after_payload TEXT, notes TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE manual_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT, platform TEXT NOT NULL, company_name TEXT NOT NULL, job_title TEXT,
  search_keywords TEXT NOT NULL DEFAULT '[]', source_url TEXT, tech_stack_keywords TEXT NOT NULL DEFAULT '[]',
  certificate_notes TEXT, required_action TEXT NOT NULL, requires_login INTEGER NOT NULL DEFAULT 0,
  requires_captcha INTEGER NOT NULL DEFAULT 0, apply_method TEXT, reminder_at TEXT, priority INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'PENDING', linked_job_id INTEGER REFERENCES jobs(id) ON DELETE SET NULL,
  notes TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_jobs_published ON jobs(status, deadline);
CREATE INDEX idx_jobs_filters ON jobs(company_type, city, job_type, source_trust_level);
CREATE INDEX idx_review_status ON review_items(status);
CREATE INDEX idx_runs_started ON collection_runs(started_at DESC);

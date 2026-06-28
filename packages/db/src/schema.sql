CREATE TABLE IF NOT EXISTS sources (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  base_url TEXT,
  adapter_type TEXT,
  mode TEXT,
  compliance_status TEXT,
  trust_level TEXT DEFAULT 'unknown',
  scope TEXT,
  enabled INTEGER NOT NULL DEFAULT 1 CHECK (enabled IN (0, 1)),
  crawl_frequency TEXT,
  last_run_at TEXT,
  last_status TEXT,
  notes TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS crawl_tasks (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  source_id INTEGER REFERENCES sources(id) ON DELETE SET NULL,
  trigger_type TEXT NOT NULL,
  status TEXT NOT NULL,
  started_at TEXT,
  finished_at TEXT,
  discovered_count INTEGER NOT NULL DEFAULT 0,
  review_item_count INTEGER NOT NULL DEFAULT 0,
  error_count INTEGER NOT NULL DEFAULT 0,
  error_summary TEXT
);

CREATE TABLE IF NOT EXISTS review_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  source_id INTEGER REFERENCES sources(id) ON DELETE SET NULL,
  task_id INTEGER REFERENCES crawl_tasks(id) ON DELETE SET NULL,
  candidate_payload TEXT NOT NULL,
  normalized_payload TEXT,
  raw_snapshot_id INTEGER,
  dedupe_key TEXT,
  confidence REAL,
  status TEXT NOT NULL DEFAULT 'pending',
  review_notes TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jobs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  company_type TEXT,
  company_name TEXT NOT NULL,
  business_unit TEXT,
  title TEXT NOT NULL,
  normalized_role TEXT,
  job_family TEXT,
  job_type TEXT NOT NULL,
  campus_batch TEXT,
  graduation_window TEXT,
  city TEXT,
  country_region TEXT,
  remote_policy TEXT,
  start_date TEXT,
  deadline TEXT,
  status TEXT NOT NULL DEFAULT 'new',
  priority INTEGER NOT NULL DEFAULT 0,
  tech_stack TEXT NOT NULL DEFAULT '[]',
  certificate_requirements TEXT NOT NULL DEFAULT '[]',
  qualification_notes TEXT,
  education_requirement TEXT,
  major_requirement TEXT,
  language_requirement TEXT,
  experience_requirement TEXT,
  salary_range TEXT,
  jd TEXT,
  requirements TEXT,
  source_id INTEGER REFERENCES sources(id) ON DELETE SET NULL,
  source_url TEXT,
  apply_url TEXT,
  apply_method TEXT,
  requires_login INTEGER NOT NULL DEFAULT 0 CHECK (requires_login IN (0, 1)),
  requires_captcha INTEGER NOT NULL DEFAULT 0 CHECK (requires_captcha IN (0, 1)),
  resume_format TEXT,
  custom_questions_schema TEXT,
  source_trust_level TEXT DEFAULT 'unknown',
  dedupe_key TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_snapshots (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  job_id INTEGER REFERENCES jobs(id) ON DELETE CASCADE,
  source_id INTEGER REFERENCES sources(id) ON DELETE SET NULL,
  source_url TEXT,
  content_hash TEXT,
  raw_content TEXT,
  parsed_payload TEXT,
  attachment_paths TEXT NOT NULL DEFAULT '[]',
  captured_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS manual_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  platform TEXT NOT NULL,
  company_name TEXT NOT NULL,
  job_title TEXT NOT NULL,
  search_keywords TEXT NOT NULL DEFAULT '[]',
  source_url TEXT,
  tech_stack_keywords TEXT NOT NULL DEFAULT '[]',
  certificate_notes TEXT,
  required_action TEXT NOT NULL,
  requires_login INTEGER NOT NULL DEFAULT 0 CHECK (requires_login IN (0, 1)),
  requires_captcha INTEGER NOT NULL DEFAULT 0 CHECK (requires_captcha IN (0, 1)),
  apply_method TEXT,
  reminder_at TEXT,
  priority INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'pending',
  linked_job_id INTEGER REFERENCES jobs(id) ON DELETE SET NULL,
  notes TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  entity_type TEXT NOT NULL,
  entity_id INTEGER NOT NULL,
  action TEXT NOT NULL,
  before_payload TEXT,
  after_payload TEXT,
  notes TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_jobs_company_name ON jobs(company_name);
CREATE INDEX IF NOT EXISTS idx_jobs_title ON jobs(title);
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);
CREATE INDEX IF NOT EXISTS idx_jobs_dedupe_key ON jobs(dedupe_key);
CREATE INDEX IF NOT EXISTS idx_manual_items_status ON manual_items(status);
CREATE INDEX IF NOT EXISTS idx_manual_items_platform ON manual_items(platform);
CREATE INDEX IF NOT EXISTS idx_review_items_status ON review_items(status);

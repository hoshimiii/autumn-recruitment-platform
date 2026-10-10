CREATE TABLE agent_settings (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  enabled INTEGER NOT NULL DEFAULT 0 CHECK (enabled IN (0, 1)),
  base_url TEXT NOT NULL,
  secret_env_ref TEXT NOT NULL,
  scan_model TEXT NOT NULL,
  scan_reasoning_effort TEXT NOT NULL,
  process_model TEXT NOT NULL,
  process_reasoning_effort TEXT NOT NULL,
  timeout_seconds INTEGER NOT NULL,
  config_version INTEGER NOT NULL DEFAULT 1,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO agent_settings (id, base_url, secret_env_ref, scan_model, scan_reasoning_effort,
  process_model, process_reasoning_effort, timeout_seconds)
VALUES (1, 'http://127.0.0.1:18080/v1', 'SUB2API_API_KEY', 'gpt-5.6-terra', 'high',
  'gpt-5.3-codex-spark', 'medium', 60);

ALTER TABLE review_items ADD COLUMN review_version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE job_projection_outbox (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  job_id INTEGER NOT NULL,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at TEXT
);
CREATE INDEX idx_projection_pending ON job_projection_outbox(processed_at,id);
CREATE TRIGGER jobs_projection_insert AFTER INSERT ON jobs BEGIN
  INSERT INTO job_projection_outbox(job_id) VALUES(NEW.id);
END;
CREATE TRIGGER jobs_projection_update AFTER UPDATE ON jobs BEGIN
  INSERT INTO job_projection_outbox(job_id) VALUES(NEW.id);
END;
CREATE TRIGGER jobs_projection_delete AFTER DELETE ON jobs BEGIN
  INSERT INTO job_projection_outbox(job_id) VALUES(OLD.id);
END;
INSERT INTO job_projection_outbox(job_id) SELECT id FROM jobs;

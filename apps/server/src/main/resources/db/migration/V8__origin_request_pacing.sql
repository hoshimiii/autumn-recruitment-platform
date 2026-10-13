CREATE TABLE origin_request_pacing (
 origin TEXT PRIMARY KEY, last_read_at INTEGER NOT NULL, interval_millis INTEGER NOT NULL
);
-- Keep outstanding reservations on upgrade. Task origins are already normalized.
INSERT INTO origin_request_pacing(origin,last_read_at,interval_millis)
SELECT t.origin,MAX(p.next_read_at),0
FROM browser_source_pacing p JOIN browser_tasks t ON t.source_id=p.source_id
GROUP BY t.origin;
DROP TABLE browser_source_pacing;

# Test Cases

## Source Policy

- Enabled allowed source with terms evidence can run.
- Disabled source returns `source_disabled`.
- Manual-only source returns `manual_only`.
- Login-required, captcha-required, forbidden, or unknown compliance blocks collection.

## Job Normalization

- Company and title whitespace are trimmed.
- Configured array or delimited tech-stack and certificate values normalize to lists.

## Agent Workflow

- Settings default to disabled local Sub2API routing with Terra high scanning and
  Codex Spark processing.
- Settings accept only a loopback `/v1` endpoint and an environment-variable name.
- API responses expose only `secretConfigured`, never the secret value.
- Connectivity checks parse `/models`, report absent models, and never call a model.
- Global or source disablement skips Agent calls.
- Valid scanner/processor JSON is validated by the deterministic normalizer.
- Processor output cannot replace company/title identity, source URL, or application URL.
- Provider, timeout, and malformed output failures preserve deterministic candidates
  with fallback evidence.

## Deduplication

- Canonical source URL and content hash identify exact repeats.
- Business key uses normalized company, title, city, job type, batch, and deadline.
- A changed content hash creates review while the business key targets the same job.

## Review

- Approve creates a job and snapshot.
- Approving changed content updates the existing job and logs before and after payloads.
- Reject prevents a review item from appearing in jobs.
- Needs manual follow-up moves the review item to `BLOCKED`.

## Manual Items

- Required fields validate platform, company or keywords, source URL, required action, login flag, captcha flag, and status.
- Manual item creation does not enqueue collection.

## Persistence

- SQLite stores jobs, sources, runs, review items, snapshots, manual items, and audit logs.
- Flyway upgrades an existing V4 database to V5 without changing published migration checksums.

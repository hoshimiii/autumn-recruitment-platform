# User Stories

## Source Configuration

As a local-first recruitment tracker user, I can add or enable a source with source name, source type, base URL, company or category scope, collection mode, compliance status, adapter type, crawl frequency, and last run status so collection stays source-scoped and auditable.

Only enabled `ALLOWED` sources with a terms URL and recorded confirmation can
be collected automatically.

## Agent Configuration

As an operator, I can configure the local Sub2API endpoint, secret environment
variable, scanner model and reasoning level, and processor model and reasoning
level. I can check connectivity and model catalog availability without invoking
a model or revealing the secret.

Agent execution remains off until I enable it globally and on an individual
source. A provider or output-validation failure keeps the deterministic candidate
and records fallback evidence for review.

## Controlled Collection

As a user, I can trigger one-shot collection or allow scheduled collection for enabled whitelist sources so new or changed jobs are discovered without broad crawling.

Collection loads enabled whitelist sources, skips manual-only, forbidden,
login-required, captcha-required, and unknown compliance sources, runs the
matching adapter, preserves bounded candidate evidence, normalizes jobs,
computes dedupe keys and content hashes, writes new or changed records to the
review queue, and records timing, counts, and errors.

For an Agent-enabled source, the scanner attaches relevance evidence without
dropping candidates and the processor maps bounded enrichment fields to the
canonical job template. Deterministic validation remains authoritative at the
trust boundary.

Collection is low-frequency, bounded, source-scoped, and snapshot-based. Failed sources do not block other sources.

## Review Queue

As a user, I can inspect candidate evidence, approve it, reject it, or mark it
as needing manual follow-up. Approval updates an existing job when the dedupe
key matches.

All review actions create audit logs. Every new or changed record enters review
before becoming an official job.

## Job Table

As a user, I can scan and filter jobs by keyword, company type, company, job family, job type, city, tech stack, certificate or qualification, deadline status, source trust level, application status, and review status.

Visible columns include type, company, job title, job type, city, tech stack summary, certificate or qualification summary, start date, deadline, status, source, trust level, and priority.

## Job Detail

As a user, I can open a job and see company, title, job type, city, source,
responsibilities, requirements, tech stack, certificates, application method,
URLs, login or captcha requirements, confidence, capture time, and snapshots.

Certificate and qualification requirements distinguish hard requirement, bonus point, unknown, and not mentioned.

## Manual Actions

As a user, I can record manual-only sources with platform name, company, job title or search keywords, source URL, tech stack keywords, certificate notes, required action, login requirement, captcha requirement, application method, reminder time, priority, status, and notes.

The form creates a standalone manual item. Manual-only records never trigger
automatic collection.

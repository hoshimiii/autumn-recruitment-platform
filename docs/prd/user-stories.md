# User Stories

## Source Configuration

As a local-first recruitment tracker user, I can add or enable a source with source name, source type, base URL, company or category scope, collection mode, compliance status, adapter type, crawl frequency, and last run status so collection stays source-scoped and auditable.

Only allowed or review-required sources can be collected automatically.

## Controlled Collection

As a user, I can trigger one-shot collection or allow scheduled collection for enabled whitelist sources so new or changed jobs are discovered without broad crawling.

Collection loads enabled whitelist sources, skips manual-only, forbidden, login-required, captcha-required, and unknown compliance sources, runs the matching adapter, stores raw snapshots and attachments, normalizes candidate jobs, computes dedupe keys and change summaries, writes new or changed records to the review queue, and records timing, counts, and errors.

Collection is low-frequency, bounded, source-scoped, and snapshot-based. Failed sources do not block other sources.

## Review Queue

As a user, I can approve a candidate as a new job, merge it into an existing job, reject it, mark it as needing manual follow-up, or edit normalized fields before approval.

All review actions create audit logs. New or changed records enter review before becoming official jobs unless they come from a high-confidence adapter and pass validation rules.

## Job Table

As a user, I can scan and filter jobs by keyword, company type, company, job family, job type, city, tech stack, certificate or qualification, deadline status, source trust level, application status, and review status.

Visible columns include type, company, job title, job type, city, tech stack summary, certificate or qualification summary, start date, deadline, status, source, trust level, and priority.

## Job Detail

As a user, I can open a job and see company, title, type, city, batch, source, responsibilities, requirements, tech stack, certificates, education, major, language, salary, application method, URLs, login or captcha requirements, special resume format, custom questions, source snapshots, attachments, change history, review history, and application timeline.

Certificate and qualification requirements distinguish hard requirement, bonus point, unknown, and not mentioned.

## Manual Actions

As a user, I can record manual-only sources with platform name, company, job title or search keywords, source URL, tech stack keywords, certificate notes, required action, login requirement, captcha requirement, application method, reminder time, priority, status, and notes.

The form can create a manual item and optionally link it to a job after the user verifies details. Manual-only records never trigger automatic collection.

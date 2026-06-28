# API Contracts

## Jobs

`GET /api/jobs` lists approved jobs with filters for keyword, company type, company, job family, job type, city, tech stack, certificate or qualification, deadline status, source trust level, application status, and review status.

Response includes `items`, `page`, `pageSize`, and `total`.

`GET /api/jobs/:id` returns a job detail record with identity fields, JD, requirements, tech stack, certificate requirements, education, major, language, salary, application method, source URLs, login and captcha flags, resume format, custom questions, snapshots, change history, review history, and application timeline.

## Sources

`GET /api/sources` lists configured sources with compliance status, adapter type, enabled state, frequency, last run time, and last status.

Collection mutations only operate on sources allowed by source policy.

## Crawl Tasks

`GET /api/tasks` lists task runs with trigger type, status, started time, finished time, discovered count, review item count, error count, and error summary.

Task creation validates source policy before invoking adapters.

## Review Items

`GET /api/review-items` lists candidates and changes pending review.

Review actions support approve as new job, merge into existing job, reject, mark needs manual follow-up, and edit normalized fields before approval. Each action writes an audit log.

## Manual Items

`GET /api/manual-items` lists manual-only records.

`POST /api/manual-items` creates a manual action record with platform, company, job title or keywords, source URL, tech stack keywords, certificate notes, required action, login flag, captcha flag, application method, reminder time, priority, status, and notes.

Manual item creation does not trigger collection.

## Error Shape

API errors return a typed JSON body containing an error code, message, and optional details. Policy-blocked collection returns a code matching the source policy reason.

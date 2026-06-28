# Acceptance Criteria

## Collection Policy

- Automatic collection runs only for enabled whitelist sources whose policy allows collection.
- Disabled, manual-only, forbidden, login-required, captcha-required, and unknown compliance sources are skipped.
- `source.compliance_status = unknown` blocks automatic collection until the source is classified.
- Unknown or low-confidence pages and parse results discovered from a known compliant source go to review instead of direct approval.
- Collection is bounded to configured sources and records task status, timing, discovered count, review item count, error count, and error summary.
- Failed sources do not block remaining sources in the same run.

## Review And Job Records

- New or changed candidates create review items unless a high-confidence adapter and validation rules permit direct approval.
- Approve creates or updates an official job and preserves raw source context.
- Merge updates an existing job and logs the action.
- Reject prevents a candidate from appearing in the job table.
- Each review action creates an audit log.

## Workbench

- The first screen is the workbench, not a landing page.
- The job table displays type, company, title, job type, city, tech stack, certificate or qualification summary, deadline, status, source trust, and priority.
- Filters include keyword, company type, company, job family, job type, city, tech stack, certificate or qualification, deadline status, source trust, application status, and review status.
- The detail page shows JD, requirements, tech stack, certificate or qualification requirements, source snapshots, application method, and login or captcha requirements.

## Manual Records

- A manual item stores platform, company, job title or keywords, source URL, required action, login flag, captcha flag, reminder, priority, status, and notes.
- Manual-only records can be linked to a verified job.
- Manual-only records do not start crawlers or source adapters.

## Local-First Operation

- SQLite is the local persistence layer.
- Data is migration-based and can be backed up as local files.
- The MVP remains single-user and local-first.

# Acceptance Criteria

## Collection Policy

- Automatic collection runs only for enabled whitelist sources whose policy allows collection.
- Disabled, manual-only, forbidden, login-required, captcha-required, and unknown compliance sources are skipped.
- `source.compliance_status = unknown` blocks automatic collection until the source is classified.
- Unknown or low-confidence pages and parse results discovered from a known compliant source go to review instead of direct approval.
- Collection is bounded to configured sources and records task status, timing, discovered count, review item count, error count, and error summary.
- Failed sources do not block remaining sources in the same run.

## Review And Job Records

- Every new or changed candidate creates a review item in the first delivery.
- Approve creates or updates an official job and preserves raw source context.
- Merge updates an existing job and logs the action.
- Reject prevents a candidate from appearing in the job table.
- Each review action creates an audit log.
- Repeating unchanged collection or approval does not create duplicate jobs.
- Only reviewed `PUBLISHED` and non-expired jobs appear in the public workspace.

## Workbench

- The first screen is the workbench, not a landing page.
- The job table displays type, company, title, job type, city, tech stack, certificate or qualification summary, deadline, status, source trust, and priority.
- Filters include keyword, company type, company, job family, job type, city, tech stack, certificate or qualification, deadline status, source trust, application status, and review status.
- The detail page shows JD, requirements, tech stack, certificate or qualification requirements, source snapshots, application method, and login or captcha requirements.
- Filters are URL-backed and survive refresh.
- Loading, empty, stale, and API failure states are visible and actionable.
- The app mounts standalone and as a nested-base micro-frontend, and unmount removes its React root and listeners.

## Manual Records

- A manual item stores platform, company, job title or keywords, source URL, required action, login flag, captcha flag, reminder, priority, status, and notes.
- Manual-only records can be linked to a verified job.
- Manual-only records do not start crawlers or source adapters.

## Local-First Operation

- SQLite is the local persistence layer.
- Data is migration-based and can be backed up as local files.
- The MVP remains single-user and local-first.
- REST, CLI, and MCP use the same application services.
- CLI and MCP support job search, configured-source execution, and export.
- MCP does not expose arbitrary URL fetches, shell execution, credentials, or resume submission.
- Markdown and Excel exports use the same standard job template and filter semantics.
- The Excel export can be read back by Apache POI without repair warnings.

## Security And Compliance

- Collection uses an identifiable user agent and never impersonates a browser or user.
- URL validation blocks unsupported schemes, unapproved origins, and disallowed redirects.
- Robots denial, login/captcha detection, and `401`, `403`, or `429` stop the source run.
- Plaintext AI/API tokens are never stored or returned; only environment-variable references are persisted.
- Optional AI is disabled by default and cannot make network calls until explicitly configured and enabled.

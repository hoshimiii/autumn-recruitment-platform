# Test Plan

Implementation follows TDD for production code. Documentation and root config bootstrap are exempt from test-first production-code requirements.

## Unit Coverage

- Normalize adapter payloads into job fields.
- Parse tech stack and certificate requirements.
- Compute dedupe keys.
- Detect changed snapshots.
- Block manual-only, login-required, captcha-required, forbidden, disabled, and unknown sources.
- Approve, reject, and merge review items.
- Validate manual item required fields.

## Integration Coverage

- Crawl task creates review items for new jobs.
- Partial source failure does not fail the whole task.
- Review approval writes jobs and snapshots.
- Merge updates an existing job and logs the action.
- Manual item create, list, and update works.
- SQLite persists after restart.
- Job filters, sorting, and pagination return expected records.

## E2E Coverage

- User triggers collection, reviews a job, approves it, and sees it in the table.
- User rejects an invalid review item and it does not appear in jobs.
- User creates a manual-only platform record and no automatic collection is triggered.
- User disables a source and collection skips it.
- User opens a job detail page and sees snapshots, tech stack, certificates, and application information.

## Documentation Verification

Run the placeholder scan against docs and root config before committing documentation baseline changes.

# Test Plan

Implementation follows TDD for production code. Documentation and root config bootstrap are exempt from test-first production-code requirements.

## Unit Coverage

- Normalize adapter payloads into job fields.
- Parse tech stack and certificate requirements.
- Compute dedupe keys.
- Detect changed snapshots.
- Block manual-only, login-required, captcha-required, forbidden, disabled, and unknown sources.
- Validate URL scheme, origin, redirects, limits, and robots decisions before fetch.
- Approve, reject, and block review items.
- Validate manual item required fields.
- Render the canonical Markdown field order.
- Validate Agent settings, loopback endpoint, environment-variable reference, and
  reasoning levels.
- Parse Sub2API model catalogs and non-streaming Responses output with local stubs.

## Integration Coverage

- JSON and HTML fixture sources create review items for new jobs.
- Robots denial, login redirect, and `401`, `403`, or `429` stop collection.
- Partial source failure does not fail the whole task.
- Review approval writes jobs and snapshots.
- Changed content updates an existing job through review and logs the action.
- Unchanged reruns and repeated approval are idempotent.
- Manual item create and list works.
- SQLite persists after restart.
- Job filters, sorting, and pagination return expected records.
- Daily runs do not overlap in one process.
- Agent execution honors global and source switches; malformed/provider failures
  retain deterministic candidates and bounded fallback evidence.
- Excel exports are read back with the expected headers and values.

## E2E Coverage

- User triggers collection, reviews a job, approves it, and sees it in the table.
- Mocked browser coverage checks job display, URL-backed filters, detail, exports,
  separated operations routes, and layout overflow.
- The real Java browser path creates a compliant source, collects one fixture,
  reviews and approves it, filters the published job, opens detail, and downloads
  Markdown and Excel.
- Micro-frontend mount/unmount and source/manual/Agent settings forms are covered by frontend tests.

## Interface Coverage

- REST, CLI, and MCP search return the same records for the same filters.
- CLI run-source uses the same policy and collection service as REST.
- MCP initialization, tool listing, and one `search_jobs` call succeed over STDIO.
- MCP tool listing contains only the documented allowlist.

## Verification Order

1. Run focused unit tests while implementing each behavior.
2. Run Java integration tests with temporary SQLite and local HTTP fixtures.
3. Run frontend unit tests and type checks.
4. Run REST, CLI, MCP, and export smoke checks.
5. Run Playwright against the real Java backend.
6. Perform desktop and mobile screenshot review for hierarchy, overlap, clipping,
   empty/error states, and table readability.
7. Run the full clean build before final acceptance.

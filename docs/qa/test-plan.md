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

## Backend enhancement coverage

- OIDC tests use an isolated issuer/JWKS and actual code exchange/session flow;
  test signed JWT issuer, audience, expiry, role gates, service-level denial,
  CSRF and credentialed cross-origin logout. This is protocol fixture evidence,
  not acceptance against an external IdP or the optional Keycloak container.
- Concurrent SQL tests use parallel threads and multiple pooled connections for
  atomic candidate insertion, review decision contention and returned IDs.
- Canonical identity tests distinguish unknown department/cohort, official IDs,
  locations, stable source rows and edited review identity. Read back Markdown,
  Excel and public interfaces after approval.
- Browser task tests cover approval invalidation, leases, fencing, expiry,
  replay, evidence budgets and transactional pending ingestion. Real Chrome
  worker tests prove denied requests never dispatch and access barriers stop
  follow-up reads. A two-process provider-stub smoke runs against the Java API.
- Projection tests cover fixed bounded fuzzy query, network/Redis failure,
  refresh-failed replay with superseded versions and independent dataset
  namespaces. Real Redis/ES behavior requires a separate running middleware test.
- Analytics unit/config checks are separate from actual Hadoop/HBase/ZooKeeper
  build/start/aggregate/readback acceptance; see the isolated lab README.
- Exact department/title/cohort filters must agree across REST, CLI, MCP and
  exports; omitted optional filters must preserve records with unknown values.

Final counts, command evidence and unavailable runtime prerequisites belong in
`CONTEXT.md`; historical counts do not validate later code changes.

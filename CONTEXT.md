# Project Context

## Objective and Acceptance

Rebuild the existing MVP into a local-first autumn recruitment platform with:

- A TypeScript and React micro-frontend for daily job discovery, filters, details,
  source configuration, review, run history, and exports.
- A Java backend that collects permitted public sources, normalizes candidates,
  deduplicates them, routes them through review, and publishes approved jobs.
- Shared backend capabilities exposed through REST, CLI, and MCP.
- Markdown and Excel exports using one canonical job template.
- Automated tests and a browser-level acceptance path for the full workflow.

The delivery is complete only when a configured source can be collected, its
candidate reviewed, the resulting job shown and filtered in the web app, and the
same data exported through REST, CLI, and MCP.

## Constraints, Authority, and Do-Not-Change

- Never log in, bypass captcha, evade rate limits, or automate a source whose
  terms or robots policy forbid collection.
- Do not post content or use the user's accounts.
- Do not search for, store, or use leaked/shared "free" tokens.
- AI settings are metadata-only in this delivery and never initiate network calls.
- Keep the existing `mount`/`unmount` micro-frontend contract and base-route
  behavior unless an integration test proves a compatible replacement.
- Use SQLite for the local delivery. A multi-instance database is out of scope.
- Keep source failures isolated and preserve traceability to the original URL.
- Prefer files below 300 lines and small cohesive modules.

## Current Work

### Done

- Existing TypeScript MVP audited.
- Git remote `origin` configured for the requested GitHub repository.
- AgentMemory initialized.
- Public-source and Java/MCP options researched.
- Baseline preserved on `codex/mvp-foundation`.
- Java 17 backend, SQLite migrations, source policy, JSON/HTML adapters, review,
  publication, exports, CLI, and MCP implemented.
- React micro-frontend connected to Java REST with display and `/ops/*` routes.
- Manual records and source create/edit/delete/test/run workflows implemented.
- Obsolete Fastify API and TypeScript DB/domain packages removed.
- Real local fixture E2E closes source -> review -> publish -> filter/detail ->
  Markdown/XLSX.
- Review evidence and published job snapshot history are available through REST
  and the operations UI.
- Published Flyway V3 remains unchanged; V4 localizes seed names incrementally.
- Desktop 1440x900 and mobile 390x844 visual checks passed without page overflow,
  control overlap, or text clipping.
- Independent final review closed every P0/P1 finding and the E2E isolation P2.

### Active

- None.

### Blocked

- No project-delivery blocker. One invalid Codex internal turn-diff ref remains
  outside project refs and was not deleted.

## Reasoning State

### Confirmed

- The current Java runtime is Java 17; Maven and Gradle are not globally installed.
- Spring Boot 3.5 supports Java 17.
- Official MCP Java SDK supports STDIO without an external web framework.
- The requested GitHub repository is private and initially empty.
- All seeded official source examples are disabled and require fresh policy and
  selector review before use.

### Provisional

- Add a source-specific adapter only when JSON/HTML configuration cannot express
  a verified public source.

### Ruled Out

- Broad crawling, anti-bot evasion, login automation, automatic resume submission,
  mandatory cloud deployment, mandatory LLM extraction, and paid services.
- Reintroducing the removed Node/Fastify backend.

### Unresolved

- Live selector and policy confirmation for seeded sources.

## Decisions and Contracts

- Java 17 is the delivery baseline because it is installed locally and supported
  by Spring Boot 3.5.
- REST, CLI, and MCP call the same application services.
- Only reviewed `PUBLISHED` jobs appear on the public job page.
- New and changed candidates enter `PENDING_REVIEW`; blocked or failed runs remain
  visible in run history.
- Every published job includes original URL, source trust, capture time, review
  status, and normalized job fields.
- Scheduler runs are incremental, non-overlapping, and isolated by source.

## Verification and Remaining Risk

- Java: 42 tests passed on 2026-07-25.
- Frontend: 18 tests, typecheck, and production build passed.
- Playwright: 3 browser tests passed, including the real Java collection workflow.
- CLI search and configured-source execution smoke passed.
- MCP initialize, exact four-tool allowlist, and `search_jobs` call smoke passed.
- `npm audit --audit-level=high`: zero vulnerabilities.
- Maven package and the stable micro-frontend entry build passed.
- Desktop 1440x900 and mobile 390x844 DOM/visual checks found no page overflow,
  clipped controls, or uneven mobile navigation.
- Live websites can change selectors or policies. Fixture-backed adapter tests are
  mandatory; live checks are operational evidence, not deterministic tests.
- The scheduler lock is single-process because SQLite local delivery is
  single-instance.
- DNS is checked before and after each request, but JDK `HttpClient` does not
  expose the connected peer address; a narrow DNS rebinding TOCTOU window remains.

## Remaining Work

- None for the accepted local delivery.

## Exact Next Action

None.

## Handoff and Completion Gate

Startup and verification commands are in `README.md`. The accepted local delivery
is complete; future work begins with live source policy/selector confirmation.

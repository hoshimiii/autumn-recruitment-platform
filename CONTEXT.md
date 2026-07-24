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
- AI extraction is optional, user-configured, disabled by default, and limited
  to classification, summarization, and tagging.
- Keep the existing `mount`/`unmount` micro-frontend contract and base-route
  behavior unless an integration test proves a compatible replacement.
- Use SQLite for the local delivery. A multi-instance database is out of scope.
- Keep source failures isolated and preserve traceability to the original URL.
- Prefer files below 300 lines and small cohesive modules.

## Current Work

### Done

- Existing TypeScript MVP audited.
- Baseline tests pass after `npm ci`: 33 tests across API, web, DB, and domain.
- Git remote `origin` configured for the requested GitHub repository.
- AgentMemory initialized.
- Public-source and Java/MCP options researched.
- Baseline preserved on `codex/mvp-foundation`.

### Active

- Freeze the v2 product, API, data, compliance, and verification contracts.
- Create stage worktrees for the Java backend and React micro-frontend.

### Blocked

- Remote repository currently returns no refs and the public GitHub page returns
  404. Local work can continue; push verification remains pending.
- `git fsck` reports one invalid Codex internal turn-diff ref. It is unrelated to
  project refs and will not be deleted without explicit approval.

## Reasoning State

### Confirmed

- The current frontend uses static seed data and does not call the API.
- Existing sources, crawl tasks, and review endpoints are placeholders.
- The current Java runtime is Java 17; Maven and Gradle are not globally installed.
- Spring Boot 3.5 supports Java 17.
- Official MCP Java SDK supports STDIO without an external web framework.

### Provisional

- Use Spring Boot 3.5.x, JDBC, Flyway, Xerial SQLite, jsoup, Apache POI, and the
  official MCP Java SDK.
- Ship generic JSON and HTML source adapters first. Add a new adapter only when
  a configured source cannot be expressed by those contracts.

### Ruled Out

- Broad crawling, anti-bot evasion, login automation, automatic resume submission,
  mandatory cloud deployment, mandatory LLM extraction, and paid services.
- Keeping the Node/Fastify API after the Java replacement passes acceptance.

### Unresolved

- Final live-source selector validation for each seeded official source.
- Whether the requested GitHub repository is empty, private, or unavailable.

## Decisions and Contracts

- Java 17 is the delivery baseline because it is installed locally and supported
  by Spring Boot 3.5.
- REST, CLI, and MCP call the same application services.
- Only reviewed `PUBLISHED` jobs appear on the public job page.
- `BLOCKED`, `PARSE_FAILED`, and low-confidence candidates require review.
- Every published job includes original URL, source trust, capture time, review
  status, and normalized job fields.
- Scheduler runs are incremental, non-overlapping, and isolated by source.

## Verification and Remaining Risk

- Baseline: `npm test` passed on 2026-07-25 after dependency installation.
- Known baseline dependency audit: three high-severity findings in the old Node
  dependency tree; reassess after the Node backend is removed.
- Live websites can change selectors or policies. Fixture-backed adapter tests are
  mandatory; live checks are operational evidence, not deterministic tests.

## Remaining Work

1. Commit contracts and create stage worktrees.
2. Implement and test Java domain, persistence, REST, collection, review, export,
   CLI, and MCP.
3. Connect and refine the React micro-frontend against the Java API.
4. Run integration, browser, visual, CLI, MCP, and export acceptance.
5. Remove obsolete Node backend packages only after replacement verification.

## Exact Next Action

Commit the v2 contracts, then create backend and frontend stage worktrees.

## Handoff and Completion Gate

Before handoff, reconcile this file with Git status, active agents, test evidence,
the exact startup commands, and remaining risks. Set `Exact Next Action` to
`None` only after all acceptance criteria pass.

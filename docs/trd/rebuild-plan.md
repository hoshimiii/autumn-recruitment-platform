# React and Java Rebuild Plan

## Working Brief

### Goal

Replace the static/placeholder MVP with the smallest complete local workflow:
configure a permitted source, collect candidates, review one, publish it, query it
from the micro-frontend, and export it through REST, CLI, and MCP.

### Non-Goals

- Automatic applications, cloud multi-user operation, browser anti-bot evasion,
  broad crawling, and required AI.
- Preserving the old Node API internals after contract-compatible replacement.

### Constraints

- Java 17 local runtime, no global Maven installation, SQLite persistence.
- Existing React `mount`/`unmount` contract remains compatible.
- Source policy and network validation are trust boundaries.
- Stage changes on independent branches/worktrees and integrate only verified work.

### Acceptance

- See `docs/prd/acceptance-criteria.md` and `docs/qa/test-plan.md`.
- Final workflow must pass backend tests, frontend tests, build, CLI/MCP smoke,
  export readback, and Playwright acceptance.

## Stage Branches and Worktrees

| Stage | Branch | Worktree | Purpose |
|---|---|---|---|
| Baseline | `codex/mvp-foundation` | main history | Preserve existing MVP |
| Contract | `codex/rebuild-contract-v1` | current | Contracts and integration owner |
| Backend | `codex/java-backend-v2` | sibling worktree | Java implementation |
| Frontend | `codex/react-microfrontend-v2` | sibling worktree | API-connected UI |
| Integration | `codex/rebuild-integration-v2` | current after stage review | Final combined system |

Branches are checkpoints, not competing products. The contract branch owns all
architecture and public contracts.

## Task Slices

### 1. Contract and build foundation

Owner: root agent.

Scope:

- Product, API, architecture, data, compliance, and test contracts.
- Maven wrapper and Java module skeleton.

Acceptance:

- Java wrapper runs without global Maven.
- Empty application test starts with a temporary SQLite database.

Do not change:

- Existing micro-frontend lifecycle behavior.
- Old Node implementation before the Java baseline is runnable.

### 2. Java core and persistence

Owner: bounded implementation agent; root reviews schema and transaction boundaries.

Scope:

- Flyway migrations.
- Job, source, run, review, snapshot, audit, and manual-item records.
- Repositories and application services.
- REST job/source/review/run endpoints.

Acceptance:

- Restart-safe SQLite integration tests.
- Approve transaction creates one job and audit evidence.
- Repeated approval and repeated unchanged candidate are idempotent.

### 3. Collection workflow

Owner: bounded implementation agent.

Scope:

- Source policy, robots check, origin boundary, fetch limits.
- JSON and HTML adapters.
- Normalization, hashing, dedupe, scheduler, and run isolation.

Acceptance:

- Local fixture tests cover allowed fetch, robots denial, redirect boundary,
  `429`, parse failure, unchanged rerun, and changed content.
- One failed source does not stop another.

### 4. Export, CLI, and MCP

Owner: bounded implementation agent after stable application services.

Scope:

- Markdown and Excel exporters.
- CLI search/run/export.
- MCP STDIO tools: search, get, run source, and export.

Acceptance:

- Excel is read back by Apache POI.
- CLI and MCP results match application-service results.
- MCP exposes no arbitrary URL or shell tool.

### 5. React micro-frontend

Owner: bounded implementation agent; root owns information architecture.

Scope:

- Typed API client and query state.
- Jobs page, detail view, filters, freshness/error states.
- Source/agent config, review, run history, and export pages.
- Host-compatible API base and route base.

Acceptance:

- No production dependency on `workbenchSeed`.
- Unit tests cover filters, errors, and config validation.
- Mount/unmount and nested base route tests continue to pass.

### 6. Integration and final review

Owner: root agent plus fresh review agent.

Scope:

- Remove old Node backend only after replacement passes.
- Run full local workflow and browser visual review.
- Reconcile docs and AgentMemory.

Acceptance commands:

```text
./mvnw.cmd test
npm test
npm run lint
npm run build
npm run e2e
```

Additional evidence:

- CLI search and export output.
- MCP initialization, tool listing, and one search tool call.
- Markdown content check and Excel readback.
- Desktop and mobile screenshots with no overlap or horizontal page overflow.

## Failure and Rollback

- If a stage fails review, keep its branch and continue from the last accepted
  checkpoint instead of rewriting history.
- Never delete worktrees or branches during implementation.
- A source adapter failure is fixed behind its contract; it must not weaken
  policy or origin checks.

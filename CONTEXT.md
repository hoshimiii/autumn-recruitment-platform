# Project Context

## Objective and Acceptance

Deliver the authorized backend interview enhancements in staged Git commits:
SSO/RBAC; SQL concurrency plus Redis; schema-aware browser worker ingestion and
ES search; pending human review; optional Hadoop/HBase/ZooKeeper analytics lab.
Keep Spring Boot/React separated, canonical application services and micro-app
mount/unmount. Full acceptance requires Java/frontend/browser/CLI/MCP/exports.

## Current Brief

- Target: E:\worktrees\backend-showcase\autumn-recruitment-platform,
  branch codex/backend-showcase, starting accepted HEAD 3e422f7.
- Authorization: user requested all four enhancements, staged implementation and
  Git commits. Local code, fixtures, test services, dependency downloads and scoped
  validation are permitted. No public push, deployment, paid model invocation,
  real Feishu collection, production database changes, or access-barrier bypass.
- Original dirty worktree remains E:\codex-projects\autumn-recruitment-platform.
  Existing Agent settings/code/contracts copied and baseline committed as 57bc7dd.
  Operational SQLite and manual import drafts were not copied.
- Scope: apps/server, apps/web, existing test/scripts/contracts; labs/analytics is
  a standalone educational capability. Keep SQLite the operational default.
- Stop: secrets exposure, external access barrier, unexpected costs/production
  mutations, or new external publication. Preserve unfinished runtime evidence.
- Completion: staged commits, clean enhancement worktree, full relevant regressions
  and explicit real-runtime vs fixture/static evidence.

## Decisions and Contracts

See docs/trd/backend-showcase.md. Cohort means graduation year, optional department
and cohort remain unknown when absent; source-record replay is distinct from
cross-source match suggestions. Browser extraction ends at pending human review.
SQLite still has one writer. Redis/ES are optional derived components, analytics
is not production storage. OIDC is explicit; local owner is a loopback trust boundary.

## Pending List

1. Contracts and existing Agent baseline preserved. [done: 57bc7dd, 028f35e]
2. Canonical identity/evidence and browser task/result protocol. [implemented/tested]
3. SSO/RBAC and frontend session/capability handling. [implemented/protocol-tested]
4. Atomic SQL writes/CAS, WAL and Redis/ES projections. [implemented/fixture-tested]
5. Standalone analytics lab. [committed: 864e014; container runtime pending]
6. Exact optional department/title/cohort query filters. [done/tested]
7. Integrated core and workers committed: cb2919e, 1d0c53c; local middleware
   topology: 394bc5a. Final evidence and startup documentation ready.
8. Runtime acceptance: Docker Engine, real IdP/middleware/lab, permitted live
   Feishu final URL/selectors and actual provider remain pending prerequisites.

## Verification and Remaining Risk

- Final Java package: 83 tests, zero failures/errors/skips, 2026-10-10.
  Evidence: apps/server/target/final-java-package.log and surefire-reports.
- Final frontend: 28 tests, typecheck/lint/build passed after exact filter changes.
  Evidence: apps/server/target/final-frontend.log.
- Playwright: 3 passed against an isolated real Java backend, plus mocked desktop
  and mobile views. Screenshots visually inspected at 1280x720 and 390x844.
  Evidence: apps/server/target/final-e2e.log and ignored test-results screenshots.
- Browser worker: 12 tests passed including actual Chrome redirect/XHR/barrier
  guards. Two separate browser worker processes with a local model-protocol stub
  completed against the packaged Java API: manual=1, pendingReview=1, published=0.
  Evidence: apps/server/target/final-worker-tests.log and final-worker-smoke.log.
- HTTP redirects are explicitly rejected; configure the approved final public
  URL. Regression proves no redirect target dispatch, while a directly configured
  nested final URL preserves relative scripts and location.pathname semantics.
- MCP STDIO initialization/tools/search smoke passed; packaged CLI search passed.
  CanonicalPublicationTest reads back actual Markdown/XLSX fields after approval.
  Evidence: apps/server/target/final-mcp.log and final-cli.log.
- Analytics Java: 3 tests passed; both middleware/lab Compose configurations valid.
  Container image build and actual aggregation/HBase readback remain unverified.
- npm audit: zero vulnerabilities after bounded lockfile-only compatible fixes.
- Independent review fixes include generated-ID races, ES refresh-failed replay,
  per-request browser policy checks, logout CORS/CSRF and dataset namespaces.
- Windows excluded port 8080 and old fixture port 14300; scoped fixture moved to
  14350. README gives a process-only 18189 startup override. No machine network
  settings changed. Owned fixture backends stopped; no acceptance-port listeners
  remain. Test data was in-memory and no operational database was changed.
- Docker CLI is installed, daemon unavailable (missing dockerDesktopLinuxEngine
  pipe). Middleware/real IdP runtime cannot yet be claimed.
- OpenViking project search reachable; returned unrelated autogame entries, so live
  code and existing project documents are the evidence for this enhancement.
- Actual Keycloak SSO, Redis/ES, Hadoop/HBase/ZooKeeper, live Feishu selectors and
  a real model call remain unverified. Protocol fixtures are not live acceptance.
- SQLite still has one writer; no throughput benchmark or distributed-database
  performance claim. JDK HttpClient DNS TOCTOU remains a limitation.

## Exact Next Action

Review the committed implementation in this named worktree. For real-runtime
acceptance, first provide a running Docker Engine, then use infra/README.md and
labs/analytics/run.ps1 smoke in the isolated local test environment. Live
Feishu and actual model calls require approved final URL/policy/selectors and
provider setup/cost authorization. No public push or operational DB migration done.

# Project Context

## Objective and Acceptance

Deliver the authorized backend interview enhancements in staged Git commits:
SSO/RBAC; SQL concurrency plus Redis; schema-aware browser worker ingestion and
ES search; pending human review; optional Hadoop/HBase/ZooKeeper analytics lab.
Keep Spring Boot/React separated, canonical application services and micro-app
mount/unmount. Full acceptance requires Java/frontend/browser/CLI/MCP/exports.

## Current Brief

- Completed PR delivery authorization (2026-10-11): user explicitly requested a
  merge request for these changes. Target only GitHub repository
  hoshimiii/autumn-recruitment-platform, head codex/backend-showcase, base main.
  Push the accepted commits and scoped continuity updates, create/update this PR
  and verify its remote head. No merge, deployment, force push, paid operation or
  changes to other branches. Stop on secret exposure, changed target or conflict.
  Completion: an open PR attached to this task and remote head matching local HEAD.

- Completed runtime acceptance (2026-10-10): user offered to start Docker Desktop;
  live Docker Engine 29.3.1 is reachable. Use only local Compose projects
  recruitops-middleware-demo, uniquely named recruitops-oidc-smoke-* provider
  projects and autumn-analytics-lab, synthetic data and owned
  fixture servers. Image/dependency downloads, ephemeral demo users and scoped
  retries are in scope; run stages sequentially and remove owned containers at
  closeout. No paid provider, public push, real source scraping or business DB
  mutation. Stop on unrelated container/port conflicts or unexpected resources.

- Target: E:\worktrees\backend-showcase\autumn-recruitment-platform,
  branch codex/backend-showcase, starting accepted HEAD 3e422f7.
- Authorization: user requested all four enhancements, staged implementation and
  Git commits. Local code, fixtures, test services, dependency downloads and scoped
  validation are permitted; the same-repository branch push and PR are now
  explicitly authorized. No merge, deployment, paid model invocation,
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
3. SSO/RBAC and frontend session/capability handling. [implemented; actual Keycloak acceptance passed]
4. Atomic SQL writes/CAS, WAL and Redis/ES projections. [implemented; actual Redis/ES passed]
5. Standalone analytics lab. [committed: 864e014; actual container runtime passed]
6. Exact optional department/title/cohort query filters. [done/tested]
7. Integrated core and workers committed: cb2919e, 1d0c53c; local middleware
   topology: 394bc5a. Final evidence and startup documentation ready.
8. Runtime acceptance: actual Redis/ES, analytics and Keycloak passed; owned
   containers/network and Java processes removed. Live Feishu final URL/selectors
   and actual provider remain pending.
9. Branch pushed and open PR #1 created against main, attached to this task:
   https://github.com/hoshimiii/autumn-recruitment-platform/pull/1
   GitHub reports MERGEABLE/CLEAN, with no configured status checks. No merge done.

## Verification and Remaining Risk

- Final Java package: 83 tests, zero failures/errors/skips, 2026-10-10.
  Evidence: apps/server/target/final-java-package.log and surefire-reports.
- Final frontend: 28 tests, typecheck/lint/build passed after exact filter changes.
  Evidence: apps/server/target/final-frontend.log.
- Actual Redis/ES runtime PASS (2026-10-10): Redis8.2.10 cache key/read hit,
  same exact/fuzzy query invalidation after approved SQL update and
  SQL fallback after stopping Redis; ES9.5.4 fuzzy match, refresh/ack, monotonic
  versioned update and literal SQL fallback after stopping ES. Synthetic source
  collected/reviewed through actual Java REST services, isolated memory dataset.
  Evidence: apps/server/target/runtime-cache-search-smoke.log; reusable command
  node scripts/middleware-smoke.mjs. Owned middleware containers/network removed;
  the unrelated agent-platform-db container remains running.
- Actual Keycloak26.7.5 runtime PASS (2026-10-10): real HTTP authorization-code
  login for VIEWER/COLLECTOR/REVIEWER/ADMIN, role-based reads/writes, session CSRF,
  actual COLLECTOR bearer from a second code exchange and backend-session logout.
  Synthetic users, random in-memory credentials and an isolated in-memory SQL
  dataset only. Evidence: apps/server/target/runtime-oidc-smoke.log; reusable
  node scripts/oidc-runtime-smoke.mjs. Owned JVM and unique provider project
  removed. This is HTTP/provider acceptance; browser OIDC CORS/SameSite remains
  unverified. Initial anonymous denial assertion was widened to 401/403 because
  embedded error dispatch differs from MockMvc; authenticated CSRF remains403.
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
  Actual container PASS: Hadoop3.4.2 local MapReduce maps six synthetic records
  and produces four aggregates; HBase2.6.6 reads back each expected count
  (1,1,3,1) via ZooKeeper3.9.6. Both Apache SHA-512 checks passed. Evidence:
  labs/analytics/target/acceptance/docker-smoke-build-attempt2.log and
  docker-smoke-attempt4.log. A transient apt500 required bounded apt retries;
  Docker Hub EOF was resolved by pulling the identical pinned ZooKeeper digest.
  Lab containers/network removed and unrelated agent-platform-db retained.
- npm audit: zero vulnerabilities after bounded lockfile-only compatible fixes.
- Independent review fixes include generated-ID races, ES refresh-failed replay,
  per-request browser policy checks, logout CORS/CSRF and dataset namespaces.
- Windows excluded port 8080 and old fixture port 14300; scoped fixture moved to
  14350. README gives a process-only 18189 startup override. No machine network
  settings changed. Owned fixture backends stopped; no acceptance-port listeners
  remain. Test data was in-memory and no operational database was changed.
- Docker Engine29.3.1 now reachable after user startup. Actual Redis/ES acceptance
  passed, as have isolated analytics and Keycloak acceptance. Final Docker inventory
  contains only the pre-existing healthy agent-platform-db container. No owned
  test networks or listeners on18180/18189/16379/19200 remain. Downloaded images
  and build cache are retained; C: has approximately5.5GiB free at closeout.
- OpenViking project search reachable; returned unrelated autogame entries, so live
  code and existing project documents are the evidence for this enhancement.
- Live Feishu selectors, actual provider calls and browser OIDC CORS/SameSite
  remain unverified. Protocol fixtures are not live source/model acceptance.
- SQLite still has one writer; no throughput benchmark or distributed-database
  performance claim. JDK HttpClient DNS TOCTOU remains a limitation.
- PR preflight: GitHub API verified main at accepted3e422f7. Normal fetch failed
  because of a broken app-internal refs/codex/turn-diffs/checkpoints reference;
  it was left untouched. Explicit scoped branch push and PR creation succeeded.
  Repairing that internal fetch reference is separate from this delivery.

## Exact Next Action

None for the authorized implementation, acceptance and PR delivery. PR #1 is
ready for review; merging requires a separate instruction. Reproduce with
infra/README.md and labs/analytics/run.ps1 smoke. The optional live-source phase
needs an approved final public Feishu URL/policy/selectors and provider setup/cost
authorization. No merge, deployment or operational DB migration done.

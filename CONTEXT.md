# Project Context

## Objective and Acceptance

Deliver the authorized backend interview enhancements in staged Git commits:
SSO/RBAC; SQL concurrency plus Redis; schema-aware browser worker ingestion and
ES search; pending human review; optional Hadoop/HBase/ZooKeeper analytics lab.
Keep Spring Boot/React separated, canonical application services and micro-app
mount/unmount. Full acceptance requires Java/frontend/browser/CLI/MCP/exports.

## Current Brief

- Pacing repair in progress (2026-10-13): user moved development to the dot cloud
  computer. Isolated branch `codex/fix-origin-pacing` starts at main
  `3bd987abc852ddd31f2aa491136f2ebddfe28e59`; baseline bundle and incoming patch
  are retained. User subsequently authorized committing and pushing this partial
  repair to `codex/fix-origin-pacing` in the same repository (2026-10-13).
  No PR creation, merge, deployment or live-source collection is authorized.
  Scope: PR #1 discussions 4241077162 and 4241077164. The remaining pacing holes
  below are relevant to this scope; this is not a fully accepted repair.
- Current patch unifies SQLite origin reservations across robots, browser and
  ordinary HTTP reads; replaces the post-wait robots fetch with a network-free
  lease/policy check. V8 retains old pending reservations. Cloud review added
  in-SQL timestamp sampling after write-lock acquisition, plus boundary tests.
- Verification is partial, not final acceptance. Before migration, the desktop
  version passed 23 affected Java tests and 11 real-Chrome policy tests. That
  evidence predates the cloud timestamp correction and added tests. Cloud passed
  28 frontend tests, lint/typecheck, build, three protocol tests, JS syntax,
  whitespace checks and supplemental real-SQLite migration/lock tests. The latter
  reproduced the old timestamp at -300 ms and corrected it to +33 ms relative
  to lock release; they do not substitute for Java/Flyway integration tests.
- Cloud Java dependency download was interrupted by cancelled approval review;
  new Java tests and full Java/package/CLI/MCP/export checks were not run.
  Cloud browser launch fails on a denied local process socket even after the
  reviewed retry; the full worker run therefore has 20 failures. E2E additionally
  cannot start its Windows-only Maven command on Linux. No browser pass is claimed.
- Known limits: newly learned robots delay is task-local until its next resource
  reservation; another same-origin source can still use its lower delay meanwhile.
  Reserved slots do not guarantee actual spacing after an arbitrarily delayed
  worker dispatch. Both are retained explicitly rather than claiming a full
  distributed rate-limit solution. Next: run final Java/browser acceptance in a
  permitted runtime and review these limits before publication.

- Active review-fix and merge authorization (2026-10-11): user requested repair
  of Codex findings followed by merging the same PR #1 into main. Scope only
  legacy identity compatibility, browser failure classification, complete row
  coverage and lease-token redaction, their contracts/tests and continuity.
  Permitted: scoped edits/tests, ordinary push, review-thread resolution and
  merge of the verified PR head. No force push, unrelated branch changes,
  deployment, paid model, live scraping or operational DB mutation. Stop on
  failing acceptance, new material finding, conflicts or unexpected remote head.
  Completion: four findings addressed, checks passed, exact-head merge verified.

- Review fixes accepted (2026-10-11): all four Codex findings repaired and a
  fresh independent reviewer found no material issue. Full relevant acceptance
  passed. PR #1 is the authoritative merge-status record; the local final
  receipt is output/runtime/pr-merge-receipt.json (ignored). The merge proceeds
  only after the remote head matches the verified repair commit.

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
  explicitly authorized; merging PR #1 is covered by the active authorization
  above. No deployment, paid model invocation,
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
9. Branch pushed and PR #1 created against main, attached to this task:
   https://github.com/hoshimiii/autumn-recruitment-platform/pull/1
   Its remote state records final merge status; no configured CI status checks.
10. Four review findings repaired and independently accepted: preserve old job
    IDs on exact same-source identity upgrades; isolate ordinary worker failures;
    require complete row-index coverage; redact lease credentials from listings.
    Java/frontend/worker/E2E and packaged CLI/MCP/export acceptance passed.

## Verification and Remaining Risk

- Review-fix acceptance (2026-10-11): Java 91 tests, no failures/errors/skips,
  fresh packaged JAR; frontend 28 tests, lint/typecheck/build; Playwright 3 tests
  with real Java collection/review/publication/exports and visually inspected
  desktop/mobile screenshots; browser workers 25 tests including real Chrome.
  The identity regression includes a file-backed Flyway V4-to-V7 upgrade,
  unchanged-job ID/history preservation and exact optional-identity isolation.
  Browser regressions prove task-only failure versus source-level blocking,
  transactional range rejection and claim-only lease disclosure.
  Evidence: apps/server/target/review-final-java.log,
  review-final-frontend-green.log, review-final-e2e-green.log;
  output/browser-pr-node-green.log and browser-pr-java-green.log.
- Fresh packaged CLI search and MCP STDIO passed; Java tests read back canonical
  Markdown/XLSX. Two separate browser worker processes against the fresh Java
  package and a local model-protocol stub completed two tasks, yielding one
  manual item, one pending review and zero published jobs. Owned JVM stopped.
  Evidence: apps/server/target/review-final-cli-green.log,
  review-final-mcp.log and review-final-worker-smoke.log.
- Review-fix fixture maintenance: use a future deadline to avoid a date-dependent
  mock failure, and port24350 because Windows now reserves14214-14513. No machine
  network settings changed. Actual Redis/ES/Keycloak/analytics evidence below
  remains from2026-10-10; those unchanged runtimes were not repeated for this fix.
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
- Windows excluded port 8080 and old fixture ports14300/14350; scoped fixture
  uses24350. README gives a process-only18189 startup override. No machine network
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

No further implementation is required for these four findings. Deliver the
verified repair commit through PR #1 under the active merge authorization;
consult its remote state and the ignored merge receipt for final merge evidence.
Optional live-source setup, deployment and operational database migration remain
outside this action.

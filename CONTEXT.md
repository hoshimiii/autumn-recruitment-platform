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

1. Publish contracts and preserve baseline. [in progress]
2. Implement canonical identity/evidence and browser task/result protocol.
3. Implement SSO/RBAC and frontend session/capability handling.
4. Implement atomic SQL writes/CAS, WAL and Redis/ES derived capabilities.
5. Implement standalone educational analytics lab.
6. Integrate fixture workers and full acceptance; review actual changes and commit.

## Verification and Remaining Risk

- Baseline Java: 53 tests passed, 2026-10-10.
- Baseline frontend: 19 tests passed, 2026-10-10.
- npm ci reports 5 dependency vulnerabilities; investigate exact advisories before
  final acceptance rather than applying an unrelated blanket upgrade.
- Docker CLI is installed, daemon unavailable (missing dockerDesktopLinuxEngine
  pipe). Middleware/real IdP runtime cannot yet be claimed.
- OpenViking project search reachable; returned unrelated autogame entries, so live
  code and existing project documents are the evidence for this enhancement.
- Earlier accepted version: Java 42/frontend18/browser3; those historical checks
  do not validate the new implementation.
- Real source policy/selectors and JDK HttpClient DNS TOCTOU remain limitations.

## Exact Next Action

Finish written contracts, then scoped TDD implementations under root ownership.

# Backend enhancement contract

## Runtime and authority

Keep Java 17 / Spring Boot application services, separate React client, local SQLite
default, and the existing mount/unmount/base-route/API-base contracts. Middleware
is optional. Operational databases and previous worktree changes are untouched.
No paid model calls, live Feishu collection, public push, or deployment is part of
development acceptance. A source needs explicit policy approval before collection;
login, CAPTCHA, 401/403/429, robots denial, copy/export restrictions or a new origin
stop a browser task. Never bypass an access barrier.
Browser workers also reject HTTP redirects and require the configured final
public URL: fulfilling redirected HTML under the original navigation URL would
corrupt relative resource and location semantics. No redirected target is fetched.

## Identity and evidence

`businessUnit` is department; `recruitmentYear` is the graduation cohort (2027 means
2027 graduates), not capture year. Missing values remain null. Company + department
+ role + cohort is a match suggestion, not authority to merge unknown departments,
different official job IDs or locations. Source + stable row ID + item discriminator
defines replay identity; normalized content hash distinguishes changed versions.
Company-level rows go to manual_items. Independently evidenced jobs enter review_items
as PENDING_REVIEW, never directly PUBLISHED. Preserve visible text, source row ID,
capture time, source URL, worker/task IDs and immutable extraction evidence.
Published queries and exports accept optional exact `businessUnit`, `title` and
`recruitmentYear` filters alongside company. Blank department/cohort filters omit
that predicate; absent record values are not synthesized or matched as wildcards.
The same filters reach REST, CLI, MCP and the frontend URL.
An existing pre-upgrade company/title/city/source-URL key is eligible for
compatibility matching only within the same source and with exactly matching
optional department/cohort/row/official-ID values. Approval updates that job in
place and adopts the canonical key in the same transaction, preserving its ID
and history. Unknown values never match newly supplied identity fields.

## Authentication and authorization

Local mode is a trusted loopback tool; OIDC mode uses a configured issuer, authorization
code login/session and bearer tokens validated against issuer/audience. Roles are
VIEWER, COLLECTOR, REVIEWER and ADMIN. Public published job queries/exports are anonymous.
COLLECTOR can create pending candidates/tasks; REVIEWER can approve/reject; ADMIN can
change sources/Agent settings and operate projections. Never trust a submitted role.
Apply checks at application services, including REST/CLI/MCP callers. Local STDIO/CLI
is explicitly a trusted local-owner boundary; OIDC mode must not silently grant it
admin. Browser session mutations require CSRF; bearer workers do not use cookies.
Expose an auth session/capability contract to React; preserve embedded host routing.

## Concurrency and optional projections

SQLite file databases use WAL and a bounded busy timeout; SQLite still has one writer.
Use atomic conflict-aware candidate insert, optimistic review state transitions and
durable task leases/fencing. Do network/model/browser work outside SQL transactions.
Redis caches published-query results; shared source pacing and task fencing remain
in SQLite, so Redis absence cannot remove constraints or let expired workers commit.
ES contains only published-job projections; SQL remains authoritative. Use a durable
outbox and replay/rebuild, validate indexed hits against SQL status, limit fuzzy search,
and fall back to SQL if ES is unavailable. Never accept arbitrary query DSL or URL.

## Browser worker protocol

Tasks carry approved source, origin, policy version, fixed row range and budgets.
Claim and complete require worker ID and current lease token; stale claims or revoked
source policies fail. Workers use separate browser contexts and bounded visible-page
reads, return validated structured fields/evidence, and stop on access barriers.
Robots checks, browser resources and ordinary HTTP collection share atomic SQLite
reservations keyed by normalized origin, including across configured sources.
Each slot honors the production floor, the preceding read's interval and the
current source/crawl delay. Different origins have independent slots. A worker
checks robots before reserving the resource slot; after waiting it calls the
fenced `/check-lease` endpoint to revalidate current approval without a source read.
Fresh crawl delays may increase a task's delay; policy and lease failures still
prevent dispatch. Existing source reservations survive the pacing migration.
Reservations use SQLite's clock after write-lock acquisition so lock contention
does not create an already-expired slot. These are scheduled read slots, not an
acknowledged distributed dispatch lock: a delayed worker may miss its slot.
Fresh robots delays are retained on the checked task; propagating that new delay
to other sources before this task's next reservation remains a known limitation.
A runnable external supervisor starts at most two workers, with local fixture/provider
stubs for non-billable acceptance. This is the application's worker-agent workflow;
native Codex/CUA delegation is a separate capability and must not be falsely claimed.

## Educational analytics lab

Hadoop processes a fixed synthetic/exported dataset; HBase stores derived aggregates;
ZooKeeper coordinates the HBase lab. These are not the transactional database or an
automatic requirement of the application. Provide runnable image/build, sample input,
aggregate/readback and scoped cleanup commands. Label configuration/static checks,
protocol fixtures and actual middleware runtime evidence separately.

## Acceptance

Tests cover optional identity fields, replay, conflicts, edited identities, RBAC/CSRF,
parallel SQL insertion, competing review decisions, expired task claims, policy
revocation, oversized/invalid results, Redis/ES failure and projection replay.
Run Java, frontend, lint, build, real browser fixtures, CLI, MCP, Markdown and Excel
readback. Capture desktop/mobile evidence. Infrastructure-dependent acceptance remains
pending if its exact runtime is unavailable; do not substitute mock success.

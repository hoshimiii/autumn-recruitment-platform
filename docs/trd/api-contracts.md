# API Contracts

All endpoints use `/api/v1`. Errors use:

```json
{
  "error": {
    "code": "stable_machine_code",
    "message": "Human readable message",
    "details": {}
  }
}
```

## Jobs

`GET /api/v1/jobs` lists published jobs.

Filters: `keyword`, `companyCategory`, `company`, `businessUnit`, `title`, `recruitmentYear`, `jobType`, `city`,
`techStack`, `certificate`, `deadlineFrom`, `deadlineTo`, `sourceTrust`,
`changedSince`, `page`, and `pageSize`.

Response: `items`, `page`, `pageSize`, and `total`.

`businessUnit` (department), `title` and `recruitmentYear` (graduation cohort)
are optional exact filters shared by REST, CLI, MCP and exports. Blank values
omit the predicate; a present department/cohort filter excludes unknown values.
Keyword search uses bounded ES fuzzy matching only when the optional projection
is ready, otherwise literal SQL matching. SQL rechecks all filters and visibility.

`GET /api/v1/jobs/{id}` returns the standard job template plus source evidence
and snapshot history.

## Sources

`GET /api/v1/sources` lists configured sources.

`POST /api/v1/sources` creates a source.

`PUT /api/v1/sources/{id}` updates a source and increments its config version.

`DELETE /api/v1/sources/{id}` deletes a source configuration.

`POST /api/v1/sources/{id}/test` validates policy and extraction without
persisting candidates.

`POST /api/v1/sources/{id}/runs` starts one bounded collection run.

Secret fields accept only environment-variable references and are never returned.

## Agent Settings

`GET /api/v1/agent-settings` returns the singleton configuration, whether its
secret environment variable is currently configured, and its config version.

`PUT /api/v1/agent-settings` updates enablement, loopback `/v1` base URL,
scanner/processor models and reasoning levels, timeout, and a required secret
environment-variable reference for enablement or connection checks. The secret
value is never accepted or returned.

`POST /api/v1/agent-settings/check` reads the provider model catalog without
invoking a model and reports connectivity plus scanner/processor catalog presence.

## Crawl Tasks

`GET /api/v1/runs` lists source and daily workflow runs.

`POST /api/v1/runs/daily` starts a non-overlapping daily run.

## Review Items

`GET /api/v1/review-items` lists only `PENDING_REVIEW` candidates and changed jobs.

`GET /api/v1/review-items/{id}` returns candidate, normalized fields, source
evidence, content hash, and a matching published job ID when dedupe finds one.

`POST /api/v1/review-items/{id}/approve` accepts optional edited normalized fields.

`POST /api/v1/review-items/{id}/reject` requires a reason.

`POST /api/v1/review-items/{id}/block` records a manual follow-up reason.

## Manual Items

`GET /api/v1/manual-items` lists manual-only records.

`POST /api/v1/manual-items` creates a record without triggering collection.

## Exports

`GET /api/v1/exports/jobs.md` returns Markdown for the current job filters.

`GET /api/v1/exports/jobs.xlsx` returns an Excel workbook for the same filters.

The column order is identical across REST export metadata, Markdown, Excel, CLI,
and MCP.

## Health

`GET /api/v1/health` reports application, database, and scheduler readiness
without exposing configuration secrets.

## Authentication and projections

`GET /api/v1/auth/session` returns `mode`, `authenticated`, trusted `roles`,
effective `capabilities`, `csrfToken`, `csrfHeader`, `loginUrl` and `logoutUrl`.
Published job queries, exports, health and this session endpoint allow anonymous
GET. Local mode trusts the loopback owner; OIDC mode validates its configured
issuer and audience. Browser sessions use authorization-code login and require
CSRF for mutations, including `POST /api/v1/auth/logout`. Bearer authentication
uses a separate stateless chain and always validates the JWT.

COLLECTOR operates source reads/runs, browser tasks and pending manual intake;
REVIEWER handles review decisions; ADMIN also changes sources/Agent settings,
approves browser policies and operates projections. Service-level authorization
also applies to CLI and MCP; OIDC STDIO cannot acquire an implicit ADMIN role.

`GET /api/v1/projections` returns `enabled`, `pending` and `lastError`.
`POST /api/v1/projections/rebuild` returns `enqueued`, the current SQL jobs queued
for replay. Both require ADMIN. SQL outbox events are acknowledged only after ES
refresh, and Redis cache keys include the dataset and SQL generation.

## Browser tasks

`GET /api/v1/browser-tasks` returns `{items:[...]}`. `POST` accepts `sourceId`,
zero-based `rowStart`, exclusive `rowEnd`, and `maxChars`; ranges and budgets are
bounded. ADMIN explicitly approves a source's current policy through
`POST /api/v1/browser-tasks/sources/{id}/approve` with
`{visibleBrowserApproved:true,adapter:"sub2api"}`. Fixture extraction additionally
requires the explicit test-only server flag. `POST .../sources/{id}/revoke`
revokes that policy; source changes invalidate approval.

For a fixed task ID, `POST /{id}/claim` takes `workerId` and returns a current
lease token. `POST /{id}/check-access`, `/pace`, `/stop`, and `/complete` require
that worker/token pair. `/check-request` also takes `requestUrl` and checks
current origin/robots policy before each navigation, redirect or XHR; it does
not fetch the submitted request URL. `/pace` returns the shared SQL-reserved
`readAt` timestamp and workers revalidate after waiting.

`/complete` accepts `rows` with visible evidence, source row/item identity,
capture time and approved source URL. Invalid, expired, revoked or conflicting
results are rejected transactionally. COMPANY records go to manual follow-up;
independently evidenced ROLE records go to `PENDING_REVIEW`. Replay does not
create duplicates and no endpoint automatically publishes extracted rows.

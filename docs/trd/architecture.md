# Architecture

## Runtime Shape

The repository remains a small monorepo:

```text
apps/
  web/       React and TypeScript micro-frontend
  server/    Java 17 and Spring Boot backend
tests/
  e2e/       Browser workflow acceptance
docs/        Product, technical, compliance, and test contracts
```

The obsolete Node API and TypeScript DB/domain packages were removed after the
Java replacement passed integration and browser acceptance.

## Technology Decisions

- Java 17 and Spring Boot 3.5.x: installed runtime compatibility and maintained
  Java 17 support.
- Spring Web and Bean Validation: REST and trust-boundary validation.
- Spring JDBC: explicit SQL and predictable SQLite behavior.
- Flyway: ordered schema migrations.
- Xerial SQLite JDBC: local persistence.
- JDK `HttpClient`: bounded requests, timeouts, redirects, conditional headers,
  and optional local Sub2API requests.
- jsoup: static HTML extraction with configured CSS selectors.
- Apache POI: `.xlsx` export.
- Official MCP Java SDK: local STDIO server.
- React, TypeScript, and Vite: preserve the current micro-frontend runtime.

No distributed queue, ORM, browser crawler, or required LLM is introduced.

## Module Boundaries

### Backend

- `domain`: records, enums, normalization, dedupe keys, and policy decisions.
- `application`: job query, collection, review, export, and source services.
- `infrastructure`: SQLite repositories, HTTP fetcher, robots checks, adapters,
  scheduler, MCP transport, and filesystem export.
- `web`: REST DTOs, validation, and typed error mapping.
- `cli`: command parsing that calls application services.

Domain and application code do not depend on REST, CLI, or MCP transports.

### Frontend

- `app`: shell, route ownership, providers, and micro-frontend lifecycle.
- `features/jobs`: filters, table, details, and export selection.
- `features/operations`: sources, runs, review, and agent settings.
- `api`: generated or handwritten typed DTOs and request functions.
- `shared`: small reusable controls and formatters.

## Data Flow

```text
configured source
  -> policy and URL validation
  -> robots check
  -> bounded fetch
  -> JSON or HTML adapter
  -> optional scanner and processor Agents
  -> normalization and content hash
  -> snapshot and dedupe
  -> review candidate
  -> approve/reject/manual
  -> published job
  -> REST / CLI / MCP / MD / XLSX
```

## Collection Contract

- Only `http` and `https` URLs are accepted.
- Every request must remain on the configured source origin unless an explicit
  allowed-origin entry exists.
- Redirects to login, captcha, or unapproved origins are blocked.
- Default per-origin interval is at least five seconds.
- `401`, `403`, and `429` stop the source run without evasion.
- Robots denial, a missing terms URL, or an unconfirmed terms state blocks collection.
- DNS resolution is checked before and after the request; a changed address set fails
  closed. JDK `HttpClient` does not expose the connected peer address, so a narrow
  DNS rebinding TOCTOU window remains.
- List page and item limits are configured and enforced before requests.
- Every run receives its own status and error summary.

Adapters receive fetched content; they cannot perform arbitrary network requests.

## Agent Contract

- One singleton `agent_settings` row stores enablement, a loopback `/v1` base URL,
  scanner/processor models and reasoning levels, timeout, and a secret environment
  variable name.
- `GET /models` is the only connectivity check and does not invoke a model.
- Model calls use OpenAI-compatible non-streaming Responses requests with redirects
  disabled, bounded input/output, and a Bearer value resolved only from the process
  environment.
- Agent execution requires global and source enablement. Scanner output is review
  evidence and never drops adapter candidates; processor output can populate only
  `jobFamily`, `jobType`, `city`, `deadline`, `jd`, `requirements`, `applyMethod`,
  `techStack`, `certificateRequirements`, `priority`, `requiresLogin`, and
  `requiresCaptcha`. It cannot replace company/title identity or source/application URLs.
- Agent output is untrusted JSON. The existing deterministic normalizer validates
  required fields and URLs; any provider or validation error falls back to the
  original adapter candidate with bounded fallback evidence.
- Agent configuration and results never bypass source policy, review, or publication
  rules.

## Review and Publication Contract

Candidate states:

```text
DISCOVERED -> PENDING_REVIEW -> PUBLISHED
                          \-> REJECTED
                          \-> BLOCKED
                          \-> PARSE_FAILED
```

- Missing company, title, source URL, or source trust cannot auto-publish.
- The first release routes all new candidates through review.
- Approval is idempotent and writes job, snapshot, and audit records atomically.
- Changed content creates a new snapshot and review candidate, not a duplicate job.

## Interface Contract

REST is the external data contract. CLI and MCP adapt the same application result
objects; they do not call REST over localhost.

MCP allowlist:

- `search_jobs`
- `get_job`
- `run_configured_source`
- `export_jobs`

No arbitrary URL fetch, shell command, credential, or resume submission tool is
exposed.

## Micro-Frontend Contract

- The bundle exports `mount(container, options?)` and `unmount()`.
- Standalone mode mounts automatically.
- Embedded mode reads the host-provided base route and does not own host navigation.
- Mount/unmount is repeatable and removes React roots and listeners.
- API base URL is runtime-configurable by host data or environment.

## Failure Isolation and Audit

- Scheduler prevents overlapping daily runs in one process.
- Each source is collected independently; one failure never aborts the batch.
- Raw snapshots are bounded and retained only for configured traceability.
- Source configuration, run history, candidate evidence, review actions, snapshots,
  and published records remain traceable.
- Secrets are referenced by environment-variable name and never returned by APIs.

## Deployment Shape

Local delivery uses two processes and one SQLite file:

```text
web:    Vite development server or static assets
server: executable Spring Boot jar
db:     ./data/recruitment.sqlite
```

Free cloud deployment is optional and deferred until local acceptance. SQLite is
not used for horizontally scaled deployments.

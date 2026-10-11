# Local middleware demonstration

Redis and Elasticsearch are optional; SQLite remains authoritative. All published
ports bind loopback. This disposable development topology has no production data
or persistent volumes. ES authentication is disabled only for this loopback lab.
Images are fixed Redis 8.2.10, ES 9.5.4 and Keycloak 26.7.5; this is not a container
vulnerability scan or a claim of production hardening.

```powershell
$env:RECRUITOPS_KEYCLOAK_ADMIN_PASSWORD = '<choose an ephemeral local password>'
docker compose -f infra/compose.yml config --quiet
docker compose -f infra/compose.yml up -d redis elasticsearch
$env:RECRUITOPS_REDIS_ENABLED = 'true'
$env:SPRING_DATA_REDIS_PORT = '16379'
$env:RECRUITOPS_ELASTICSEARCH_ENABLED = 'true'
.\mvnw.cmd -f apps/server/pom.xml spring-boot:run
```

Published job changes create SQL outbox entries in the same transaction. A bounded
projection worker refreshes ES before acknowledging events. After refresh failure,
older external versions are recognized as superseded during replay. Each SQLite
dataset has its own canonical-path-derived cache/index namespace. Cache entries
include the outbox generation and UTC date; TTL is 30 seconds. Queries fall back to
SQL while the projection is pending, unavailable, or exceeds 1000 candidate IDs;
all filters/status/deadline and totals are rechecked by SQL. This limit trades
bounded resource use for literal SQL fallback on large fuzzy result sets.

ADMIN can GET `/api/v1/projections` and POST `/api/v1/projections/rebuild`. Rebuild
queues current SQL jobs; pending delete events must be retained. Do not purge the
outbox or reset a database while retaining its derived middleware state.

For OIDC, start the optional provider:

```powershell
docker compose -f infra/compose.yml --profile sso up -d keycloak
```

Open the local Keycloak admin console on port 18180, create local demo users and
assign one of the four realm roles. Obtain the generated confidential client's
secret from `recruitops-web` credentials; do not commit or log it. Use the OIDC
profile with these process environment variables:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'oidc'
$env:RECRUITOPS_OIDC_ISSUER = 'http://127.0.0.1:18180/realms/recruitops'
$env:RECRUITOPS_OIDC_AUDIENCE = 'recruitops-api'
$env:RECRUITOPS_OIDC_CLIENT_ID = 'recruitops-web'
$env:RECRUITOPS_OIDC_CLIENT_SECRET = '<generated secret>'
$env:RECRUITOPS_OIDC_FRONTEND_URL = 'http://127.0.0.1:5173/'
```

Use the same hostname for UI/API so SameSite cookies work. For embedded hosting,
set the frontend success URL to the host's actual base route and configure its
exact CORS origin. Source/config writes remain server-authorized, and session
mutations/logout require CSRF. Background collection is disabled in OIDC mode;
bearer workers need a real token with issuer, audience and scoped roles. Local
CLI/STDIO does not silently acquire admin rights in OIDC mode.

```powershell
docker compose -f infra/compose.yml --profile sso down
```

This command removes only the named demo project's containers/network. It does
not stop unrelated services. Docker daemon availability, image startup, imported
provider claims and real Redis/ES behavior require separate runtime acceptance.

## Repeatable runtime acceptance

Build the application JAR, start only the disposable Redis/ES services as above,
then run from the repository root:

```powershell
node scripts/middleware-smoke.mjs
docker compose -f infra/compose.yml --profile sso down
```

The smoke uses a new in-memory SQL dataset, a synthetic loopback JSON source and
an owned Java process on port 18189. It collects and approves one job, proves an
actual Redis cache hit and invalidation of the same query after an update,
exercises ES fuzzy matching and versioned updates, then
stops those two demo services and asserts SQL outage fallback. It intentionally
leaves them stopped; the final Compose `down` removes their containers/network.
Do not run it against a demo stack shared by another application. No operational
database, real source, provider key or paid model is used. Server diagnostics go
to ignored `output/runtime/middleware-server.log`.

After removing the cache/search demo, run the separate provider acceptance:

```powershell
node scripts/oidc-runtime-smoke.mjs
```

This starts only Keycloak in a fresh uniquely named Compose project, generates
temporary users and credentials in memory, and starts an owned in-memory Java
backend on port 18189. It tests authorization-code login for all four roles,
session CSRF gates, an actual collector bearer token and backend-session logout.
It removes its owned JVM, provider container and network even after a failed
assertion. No credential/token values are persisted or printed. This validates
HTTP authorization-code and backend behavior; browser CORS/SameSite behavior
needs separate browser acceptance. Run sequentially with the analytics lab to
keep memory usage bounded. Existing services and occupied ports cause refusal.

# Agent Handoff

Updated: 2026-07-25

`CONTEXT.md` at the repository root is the authoritative, current project memory.
Read it before implementation or delegation.

Stable rules:

- React/TypeScript/Vite micro-frontend under `apps/web`.
- Java 17/Spring Boot/SQLite backend under `apps/server`.
- Display owns `/`; source configuration, add/import, review, and runs own
  `/ops/*`.
- `mount(container)` and `unmount()` are the micro-frontend lifecycle.
- REST, CLI, and MCP call the same Java application services.
- Sources default disabled; automatic collection requires `ALLOWED` compliance,
  an absolute terms URL, and a recorded confirmation time.
- Never bypass login, captcha, robots, rate limits, or origin/address policy.
- Secrets are environment-variable references only.
- The old Fastify and TypeScript DB/domain implementation has been removed.

Historical design notes and the superseded MVP plan remain under `docs/` only for
provenance; they are not current implementation instructions.

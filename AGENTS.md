# Repository Rules

## Product Boundary

- Build a local-first autumn recruitment information workflow.
- Public job display, source configuration, collection, review, export, CLI, and
  MCP must share one canonical job contract.
- Do not implement automatic resume submission.

## Collection Safety

- Collect only configured public sources with confirmed policy.
- Never bypass login, captcha, robots rules, rate limits, paywalls, or access controls.
- Never impersonate a user or browser. Use an identifiable application user agent.
- Stop a source on `401`, `403`, `429`, login redirects, captcha indicators, or
  an unapproved origin.
- Never store plaintext provider tokens. Persist only environment-variable names.
- Optional AI remains disabled by default.

## Architecture

- Frontend: React, TypeScript, Vite, and the existing micro-frontend lifecycle.
- Backend: Java 17, Spring Boot 3.5.x, Spring JDBC, Flyway, and SQLite.
- REST, CLI, and MCP call the same Java application services.
- Keep public contracts in `docs/prd` and `docs/trd`; update them before changing
  contract behavior.
- Prefer files below 300 lines and reuse existing modules before adding abstractions.

## Micro-Frontend Contract

- Preserve exported `mount` and `unmount`.
- Preserve standalone auto-mount and host-provided base-route behavior.
- Host and standalone API base URLs must be runtime-configurable.
- Embedded routing must not take ownership of the host application's routes.

## Development

- Use TDD for production behavior: add the smallest failing test, implement, then
  refactor only if the test stays green.
- Keep stage work isolated on `codex/*` branches and named worktrees.
- The old Node backend was removed after Java integration acceptance; do not restore it.
- Preserve unrelated user changes and do not rewrite published history.

## Verification

Run the smallest relevant checks while editing, then the full applicable set:

```text
.\mvnw.cmd test
npm test
npm run lint
npm run build
npm run e2e
```

Final acceptance also requires CLI, MCP STDIO, Markdown, Excel readback, desktop,
and mobile browser evidence.

## Continuity

- `CONTEXT.md` is the current project source of truth.
- Reconcile it after material decisions, verification changes, handoff, and final
  acceptance.
- AgentMemory records are supporting context; current code, tests, and contracts
  take precedence.

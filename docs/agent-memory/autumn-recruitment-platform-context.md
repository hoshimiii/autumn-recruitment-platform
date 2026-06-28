# Agent Memory: Autumn Recruitment Platform

Updated: 2026-06-28

## Current Objective

Continue the local-first autumn recruitment platform toward a final MVP. The latest user request is to split content by route, remove operational task/review/source/manual panels from the home display, and make the web app compatible with a future `@micro-zoe/micro-app` host as a child application.

## Repository State

- Workspace: `C:\Users\ADMIN\OneDrive\文档\tools`
- Branch: `codex/mvp-foundation`
- Current clean baseline commit before UI polish: `89facae test: verify mvp foundation`
- Dev server target: `http://127.0.0.1:5173/`
- Existing app stack: npm workspaces, TypeScript, Vite React, Fastify, SQLite via `better-sqlite3`, Vitest, Playwright.

## Completed Platform Foundation

- Documentation baseline exists under `docs/prd`, `docs/trd`, `docs/qa`, `docs/governance`, and `docs/superpowers`.
- Domain package covers source policy, job normalization/dedupe/scoring, and manual action enums.
- DB package contains SQLite schema and repositories for jobs/manual records.
- API package exposes foundation routes for jobs, sources, tasks, review items, and manual items.
- Web app now has route-separated pages:
  - `/`: job display home page with summary, filters, job table, and job detail.
  - `/ops/add`: add/import work, import contract panel, manual action form, and manual records.
  - `/ops/review`: review queue and pending-review job table.
  - `/ops/config`: source configuration.
  - `/ops/tasks`: operational task queue.
- Playwright E2E exists at `tests/e2e/workbench.spec.ts`.

## Current Architecture Notes

- Routing is intentionally lightweight and local to `apps/web/src/routing`; no router dependency has been added yet.
- The child app strips `window.__MICRO_APP_BASE_ROUTE__` when embedded by a future micro-app host.
- `apps/web/src/main.tsx` exports `mount` and `unmount` lifecycle functions and only auto-mounts when `window.__MICRO_APP_ENVIRONMENT__` is false.
- `apps/web/vite.config.ts` adds React plugin, CORS headers, and `MICRO_APP_PUBLIC_PATH` support for future hosted asset paths.
- The home page should remain display-first. Operational content belongs under `/ops/*`.

## Design Direction

The user asked to load `voltagent/awesome-design-md`. That repo is available but is not a Codex skill because it lacks `SKILL.md`; installation failed by design. It was cloned to a temp folder and used as design reference material.

Use this design interpretation:

- Primary reference: `design-md/linear.app/DESIGN.md`
  - dense, precise, restrained product UI
  - hairline borders
  - small-radius panels
  - muted text hierarchy
  - limited accent usage
- Secondary reference: `design-md/airtable/DESIGN.md`
  - table readability
  - structured workflow surfaces
  - clean white canvas

Keep the app light, not dark. Avoid marketing-page styling. Avoid decorative gradients/orbs. Use restrained surfaces, better table rhythm, badges for source trust/priority/status, and responsive layouts that do not force desktop horizontal table scrolling.

## Implementation Rules

- Keep files under about 300 lines when feasible.
- Use `apply_patch` for edits.
- Do not overwrite unrelated user changes.
- Follow TDD where practical. For route or lifecycle changes, add failing unit/E2E tests before implementation.
- Existing checks to run before final:
  - `npm run lint`
  - `npm test`
  - `npm run build`
  - `npm run e2e`
- Also verify visually in the in-app browser at `http://127.0.0.1:5173/`.

## Required Review Before Final

Spawn a fresh subagent reviewer after implementation. Ask it to inspect:

- basic functionality still works
- job table, detail panel, filters, summary, side panels, and manual form placement are logical
- aesthetics are acceptable for a recruitment operations workbench
- no obvious overlap, clipping, awkward scrollbar, or bad mobile/desktop layout

Only present the result to the user after addressing any blocking reviewer findings.

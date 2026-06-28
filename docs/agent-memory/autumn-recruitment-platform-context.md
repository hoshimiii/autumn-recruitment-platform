# Agent Memory: Autumn Recruitment Platform

Updated: 2026-06-28

## Current Objective

Polish the local-first autumn recruitment platform into a visually acceptable MVP before showing it to the user. The current user request is to continue autonomously to a final product and, before final presentation, have a subagent review basic functionality and visual/layout quality, including whether each component is logically and aesthetically placed.

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
- Web app contains a workbench with:
  - left nav
  - summary band
  - job filters
  - job table
  - job detail panel
  - review queue
  - source monitor
  - task list
  - manual action form
  - visible manual records list
- Playwright E2E exists at `tests/e2e/workbench.spec.ts`.

## Current Visual Problem

The current UI is functional but visually rough. The user highlighted the job table: oversized rows, harsh card border, awkward horizontal scrollbar, weak hierarchy, basic link styling, and an unattractive selected cell/row state. The polish should make the platform feel like a quiet, utilitarian recruitment operations workbench.

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
- Follow TDD where practical. For this visual polish, add/update a Playwright test first that fails on the current desktop table overflow/layout weakness, then make CSS/component changes.
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


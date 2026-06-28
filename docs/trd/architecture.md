# Architecture

## Runtime Shape

The application is a local-first TypeScript monorepo with separate apps and packages:

- API app for HTTP routes, request validation, and service composition.
- Web app for the operational workbench.
- Domain package for source policy, job normalization, review decisions, deduplication, scoring, and manual item rules.
- Database package for SQLite connection, migrations, and repositories.

## Data Flow

1. A user enables or configures whitelist sources.
2. A one-shot or scheduled task loads enabled sources.
3. Source policy blocks disabled, manual-only, forbidden, login-required, captcha-required, and unknown sources.
4. The adapter for each allowed source collects bounded source data and attachments.
5. The system stores snapshots and normalized candidate payloads.
6. Deduplication compares canonical URL, content hash, business key, and similarity hints.
7. New or changed candidates enter review.
8. Review actions approve, merge, reject, or mark candidates for manual follow-up.
9. Approved jobs appear in the workbench and detail views.

## Failure Isolation

Each source task records its own status, counts, and errors. A failed source does not block other sources in the same collection run.

## Auditability

Source snapshots, parsed payloads, attachment paths, review actions, and state transitions are stored so users can trace approved job records back to original source material.

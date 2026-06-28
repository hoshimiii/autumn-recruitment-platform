# Technical Requirements Document

## Architecture Summary

The backend separates transport, domain logic, adapters, and persistence. The frontend calls API routes only. API routes delegate to domain services. Domain services depend on repository interfaces. SQLite persistence implements repositories. Source collection is represented by source policy, adapter contracts, task records, snapshots, review items, and audit logs.

## Module Boundaries

- `api`: HTTP routes and request validation.
- `domain/jobs`: job normalization, scoring, deduplication, and status transitions.
- `domain/review`: review actions and audit logging.
- `domain/sources`: source policy and adapter orchestration.
- `adapters`: source-specific collection implementations.
- `persistence`: SQLite repositories and migrations.
- `tasks`: task runner and scheduling.
- `shared`: schema, result types, date utilities, and URL utilities.

Files remain focused and preferably under 300 lines.

## Technical Constraints

- The MVP uses local SQLite.
- The workspace is TypeScript and npm workspaces.
- Implementation follows TDD except for documentation and root config bootstrap work.
- All adapters return normalized candidate records and metadata; adapters do not write directly to `jobs`.
- Raw snapshots and attachments are preserved for traceability.
- Low-confidence data and LLM-extracted data go through review.

## Frontend Requirements

- Components are small and reusable.
- Table columns and filters are configuration-driven where practical.
- Detail page sections are separate components.
- Nested cards and marketing layouts are avoided.
- Dense operational UI is optimized for scanning.

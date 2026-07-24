# Technical Requirements Document

## Architecture Summary

The Java backend separates web, CLI, and MCP transports from application
services, domain records, adapters, and Spring JDBC persistence. The frontend
calls REST only. Source collection is represented by source policy, adapters,
run records, review items, snapshots, and audit logs.

## Module Boundaries

- `web`: HTTP routes and request validation.
- `domain`: records and statuses.
- `application`: source policy, collection, review, query, and export services.
- `infrastructure`: Spring JDBC repositories, HTTP/robots handling, and adapters.
- `cli` and `mcp`: allowlisted transports over the same application services.

Files remain focused and preferably under 300 lines.

## Technical Constraints

- The MVP uses local SQLite.
- The frontend is a TypeScript npm workspace; the backend is Java 17.
- Implementation follows TDD except for documentation and root config bootstrap work.
- All adapters return normalized candidate records and metadata; adapters do not write directly to `jobs`.
- Bounded candidate and parsed snapshot evidence is preserved for traceability.
- Every new or changed candidate goes through review.

## Frontend Requirements

- Components are small and reusable.
- Table columns and filters are configuration-driven where practical.
- Detail page sections are separate components.
- Nested cards and marketing layouts are avoided.
- Dense operational UI is optimized for scanning.

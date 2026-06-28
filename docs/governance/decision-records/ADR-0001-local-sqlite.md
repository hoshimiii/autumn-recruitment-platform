# ADR-0001: Use Local SQLite For MVP Persistence

## Status

Accepted

## Context

The first version is single-user and local-first. It needs durable storage for sources, crawl tasks, review items, approved jobs, snapshots, manual items, and audit logs. The data should be easy to back up and should not require a managed service.

## Decision

Use local SQLite as the default persistence layer for the MVP. Manage schema changes through migrations and keep repository modules responsible for SQL details.

## Consequences

- The MVP can run locally with simple file backup.
- The schema can store structured records and JSON text columns for arrays or parsed payloads.
- SQLite is not the target for shared multi-user collaboration, so a future cloud or team version will need a separate persistence decision.

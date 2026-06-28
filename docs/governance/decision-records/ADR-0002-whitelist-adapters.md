# ADR-0002: Use Whitelist Source Adapters Instead Of Broad Crawling

## Status

Accepted

## Context

The platform needs recruitment information from government notices, official career pages, public ATS endpoints, official attachments, and trusted discovery indexes. It must avoid login bypass, captcha bypass, prohibited scraping, and unbounded crawling.

## Decision

Use a whitelist adapter model. Each source has explicit type, scope, compliance status, adapter type, collection mode, enabled state, frequency, trust level, and notes. Automatic collection runs only when source policy allows it.

## Consequences

- Collection remains bounded, auditable, and source-scoped.
- Login-required, captcha-required, forbidden, unknown, disabled, and manual-only sources are skipped by policy.
- Manual-only sources are represented as action records with reminders and notes instead of automated collection tasks.

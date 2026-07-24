# Adapter Specification

## Adapter Types

- `JSON`: reads a configured public JSON list and maps fields by JSON path.
- `HTML`: reads a configured public HTML page and extracts items with CSS selectors.
- `MANUAL`: records the source but performs no automated request.

## Contract

Adapters receive one validated source and fetched response. They return bounded
candidate payloads with normalized fields, source URL, content hash, and
confidence. They do not perform network requests or write directly to jobs.

The collector owns policy, robots, pacing, pagination, response-size limits,
deduplication, review creation, and run history.

## Rules

- Automatic adapters run only for enabled `ALLOWED` sources with terms evidence.
- JSON and HTML configuration must be JSON objects.
- `{page}` is the only pagination placeholder.
- Third-party leads require official verification before approval.
- Attachments and AI extraction are manual-only in this delivery.

## Dedupe Inputs

Exact content uses `source_id`, business dedupe key, and content hash. Approval
uses the business key to update an existing published job when content changes.

# API Contracts

All endpoints use `/api/v1`. Errors use:

```json
{
  "error": {
    "code": "stable_machine_code",
    "message": "Human readable message",
    "details": {}
  }
}
```

## Jobs

`GET /api/v1/jobs` lists published jobs.

Filters: `keyword`, `companyCategory`, `company`, `jobType`, `city`,
`techStack`, `certificate`, `deadlineFrom`, `deadlineTo`, `sourceTrust`,
`changedSince`, `page`, `pageSize`, and `sort`.

Response: `items`, `page`, `pageSize`, and `total`.

`GET /api/v1/jobs/{id}` returns the standard job template plus source evidence
and snapshot history.

## Sources

`GET /api/v1/sources` lists configured sources.

`POST /api/v1/sources` creates a source.

`PUT /api/v1/sources/{id}` updates a source and increments its config version.

`POST /api/v1/sources/{id}/test` validates policy and extraction without
persisting candidates.

`POST /api/v1/sources/{id}/runs` starts one bounded collection run.

Secret fields accept only environment-variable references and are never returned.

## Crawl Tasks

`GET /api/v1/runs` lists source and daily workflow runs.

`POST /api/v1/runs/daily` starts a non-overlapping daily run.

## Review Items

`GET /api/v1/review-items` lists candidates and changed jobs.

`GET /api/v1/review-items/{id}` returns candidate, normalized fields, source
evidence, and duplicate hints.

`POST /api/v1/review-items/{id}/approve` accepts optional edited normalized fields.

`POST /api/v1/review-items/{id}/reject` requires a reason.

`POST /api/v1/review-items/{id}/block` records a manual follow-up reason.

## Manual Items

`GET /api/v1/manual-items` lists manual-only records.

`POST /api/v1/manual-items` creates a record without triggering collection.

## Exports

`GET /api/v1/exports/jobs.md` returns Markdown for the current job filters.

`GET /api/v1/exports/jobs.xlsx` returns an Excel workbook for the same filters.

The column order is identical across REST export metadata, Markdown, Excel, CLI,
and MCP.

## Health

`GET /api/v1/health` reports application, database, and scheduler readiness
without exposing configuration secrets.

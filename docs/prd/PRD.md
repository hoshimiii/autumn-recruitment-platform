# Product Requirements Document

## Product Goal

Deliver a local-first recruitment information workflow that discovers permitted
public autumn recruitment postings each day, converts them into one standard
record, requires review when evidence is incomplete, and publishes approved jobs
to a fast filterable micro-frontend.

Initial coverage:

- Shandong central, provincial, and municipal state-owned enterprises.
- Developer roles at major internet companies.
- Developer roles at small and mid-size companies.
- Foreign companies through the same extensible source contract.

## Users and Jobs To Be Done

### Candidate

- See newly published jobs without searching many websites.
- Filter by company category, role, city, technology, certificate, deadline, and
  source trust.
- Inspect the full JD, qualifications, source evidence, and application URL.
- Export a selected result set to Markdown or Excel.

### Operator

- Configure approved sources without changing code.
- Run one source or the daily collection workflow.
- See blocked, failed, duplicate, and low-confidence candidates.
- Edit and approve a candidate or reject it with an audit trail.
- Configure an optional AI classifier without exposing secrets in stored records.

## Functional Scope

### Public job workspace

- Default page shows only `PUBLISHED` jobs that are not expired.
- New jobs and recently changed jobs are visibly distinguished.
- Filters are reflected in the URL and survive refresh.
- Selecting a row opens complete job details without losing filter state.
- Loading, empty, error, and stale-data states are explicit.

### Source and Agent configuration

- Supports `JSON`, `HTML`, and `MANUAL` modes in the first delivery.
- Stores source identity, trust level, category, URLs, schedule, rate limit,
  selectors or field mappings, keywords, robots/terms review, and enabled state.
- Optional AI configuration stores a secret reference, never a plaintext token.
- A source can be tested without publishing data.

### Collection workflow

1. Validate source policy and URL boundary.
2. Check robots policy and source state.
3. Fetch the bounded list/feed using conditional requests where available.
4. Extract and normalize candidate fields.
5. Deduplicate by source URL and business identity; detect changed content.
6. Route uncertain or changed data to review.
7. Publish only after approval and preserve source evidence.

### Review and operations

- Review actions: approve, edit and approve, reject, and mark blocked/manual.
- Run history shows source, trigger, status, counts, duration, and error summary.
- A source failure does not stop other sources.
- Re-running unchanged input is idempotent.

### Interfaces

- REST API drives the web app.
- CLI supports job search, source execution, and Markdown/Excel export.
- MCP exposes allowlisted tools for the same operations.
- REST, CLI, and MCP reuse the same application services and field template.

## Standard Job Template

Each published job contains:

- Identity: company category, company, business unit, title, normalized role.
- Recruitment: job type, batch, graduation window, start date, deadline, status.
- Location: city, region, remote policy.
- Requirements: education, major, language, experience, technology stack,
  certificates/qualifications, JD, and requirement text.
- Application: application URL/method, login/captcha flags, resume format.
- Evidence: source URL, source trust, capture time, content hash, confidence,
  review status, and update time.

## Non-Goals

- Broad internet crawling or guaranteed whole-market coverage.
- Login, captcha, anti-bot, paywall, or rate-limit circumvention.
- Posting content through the user's accounts.
- Automatic resume submission.
- Multi-user cloud collaboration.
- Mandatory LLM use or searching for shared/leaked tokens.
- Multi-instance scheduling or a distributed queue.

## Success Criteria

- A fixture-backed configured source completes the full collect-to-publish flow.
- At least one approved official-source configuration can be run locally.
- Public filters and details operate on persisted API data.
- Markdown and Excel exports contain the standard fields and can be read back.
- CLI and MCP return the same persisted records as REST.
- All automated checks and browser acceptance pass from a clean checkout.

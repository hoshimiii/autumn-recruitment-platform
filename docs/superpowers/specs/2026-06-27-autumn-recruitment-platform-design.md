# Autumn Recruitment Platform Design

Date: 2026-06-27

## Goal

Build a local-first information integration platform for autumn recruitment jobs. The first version should automatically discover and normalize jobs from approved sources, keep risky sources as manual action records, and provide a clear table plus detail workflow for screening, reviewing, and preparing applications.

## Confirmed Product Direction

The MVP is not a broad crawler. It is a whitelist source system with controlled one-shot or scheduled collection, structured parsing, deduplication, scoring, and a review queue.

Initial source categories:

- Shandong state-owned and central state-owned enterprise information.
- Internet company developer roles.
- Small and mid-size company developer roles.
- Future extension for foreign companies.

Primary collection strategy:

- Use source adapters for public official pages, government notices, public ATS APIs, official career pages, and trusted aggregator indexes.
- Use attachment parsing for Shandong SOE notices because many jobs are in Excel, Word, or PDF files.
- Use changedetection/RSS-style monitoring as a discovery layer for pages without stable APIs.
- Use LLM extraction only as an assisted parser for long notices or non-standard pages, and send results to review.
- Use manual action records for login, captcha, strong anti-automation, or terms-restricted platforms.

## Non-Goals

- Do not bypass login, captcha, rate limits, or anti-automation systems.
- Do not automatically scrape platforms whose terms prohibit automation.
- Do not implement automatic resume submission in the MVP.
- Do not build multi-user cloud collaboration in the MVP.
- Do not use broad, unbounded internet crawling.

## Source Policy

Every source has a compliance and execution policy.

Allowed automatic collection:

- Public government or SASAC notices.
- Public official career pages.
- Public ATS endpoints such as Greenhouse and Lever.
- Public attachments linked from official notices.
- Trusted public index pages when used only for discovery and official-link backtracking.

Review-required collection:

- Third-party aggregators.
- WeChat/article reposts.
- Unknown pages.
- LLM-extracted records.
- Low-confidence attachment parsing.

Manual-only records:

- Login-required pages.
- Captcha-required pages.
- Strong anti-bot or high-risk platforms.
- Platforms whose terms disallow automation.

For manual-only sources, the app stores platform name, company, job/search keywords, source URL, required manual action, reminder time, and notes. It does not run a crawler.

## Core User Flows

### Source Configuration

The user adds or enables a source with:

- Source name.
- Source type.
- Base URL.
- Company or category scope.
- Collection mode.
- Compliance status.
- Adapter type.
- Crawl frequency.
- Last run status.

Only allowed or review-required sources can be collected automatically.

### Controlled Collection

The user can trigger a one-shot collection or allow scheduled collection for enabled sources.

Flow:

1. Load enabled whitelist sources.
2. Skip manual-only, forbidden, login-required, and captcha-required sources.
3. Run the matching adapter.
4. Store raw snapshots and attachments.
5. Normalize candidate jobs.
6. Compute dedupe keys and change summaries.
7. Write new or changed records to the review queue.
8. Record task result, timing, counts, and errors.

Collection must be low-frequency, bounded, source-scoped, and snapshot-based. Failed sources do not block other sources.

### Review Queue

All new or changed records enter review before becoming official jobs unless they come from a high-confidence adapter and pass validation rules.

Review actions:

- Approve as new job.
- Merge into existing job.
- Reject.
- Mark needs manual follow-up.
- Edit normalized fields before approval.

All review actions create audit logs.

### Job Table

The job table is the primary work surface. It should be dense, scannable, and filterable.

Visible columns:

- Type.
- Company.
- Job title.
- Job type.
- City.
- Tech stack summary.
- Certificate/qualification summary.
- Start date.
- Deadline.
- Status.
- Source.
- Trust level.
- Priority.

Filters:

- Keyword.
- Company type.
- Company.
- Job family.
- Job type.
- City.
- Tech stack.
- Certificate/qualification.
- Deadline status.
- Source trust level.
- Application status.
- Review status.

### Job Detail Page

The detail page contains the full context needed to decide whether to apply.

Sections:

- Job identity: company, title, type, city, batch, source.
- JD and responsibilities.
- Requirements.
- Tech stack.
- Certificate and qualification requirements.
- Education and major requirements.
- Language requirements.
- Salary range if available.
- Application method and URL.
- Whether login, captcha, special resume format, or custom questions are required.
- Source snapshots and attachments.
- Change history.
- Review history.
- Application timeline.

Certificate and qualification requirements should distinguish:

- Hard requirement.
- Bonus point.
- Unknown.
- Not mentioned.

Examples include CET-4/CET-6, professional qualification certificates, computer rank certificates, soft exam certificates, party membership, driving license, security clearance, and work authorization or visa requirements for foreign companies.

### Manual Action Form

This form handles sources that should not be automatically collected.

Fields:

- Platform/source name.
- Company name.
- Job title or search keywords.
- Source URL.
- Tech stack keywords.
- Certificate/qualification notes.
- Required manual action.
- Login required.
- Captcha required.
- Application method.
- Reminder time.
- Priority.
- Status.
- Notes.

The form can create a manual item and optionally link it to a job after the user verifies details.

## Data Model

The MVP uses local SQLite. The schema should be migration-based and easy to back up.

### `sources`

Stores configured information sources.

Key fields:

- `id`
- `name`
- `type`
- `base_url`
- `adapter_type`
- `mode`
- `compliance_status`
- `trust_level`
- `scope`
- `enabled`
- `crawl_frequency`
- `last_run_at`
- `last_status`
- `notes`
- `created_at`
- `updated_at`

### `crawl_tasks`

Stores collection task runs.

Key fields:

- `id`
- `source_id`
- `trigger_type`
- `status`
- `started_at`
- `finished_at`
- `discovered_count`
- `review_item_count`
- `error_count`
- `error_summary`

### `review_items`

Stores candidate jobs or changes pending review.

Key fields:

- `id`
- `source_id`
- `task_id`
- `candidate_payload`
- `normalized_payload`
- `raw_snapshot_id`
- `dedupe_key`
- `confidence`
- `status`
- `review_notes`
- `created_at`
- `updated_at`

### `jobs`

Stores approved jobs.

Key fields:

- `id`
- `company_type`
- `company_name`
- `business_unit`
- `title`
- `normalized_role`
- `job_family`
- `job_type`
- `campus_batch`
- `graduation_window`
- `city`
- `country_region`
- `remote_policy`
- `start_date`
- `deadline`
- `status`
- `priority`
- `tech_stack`
- `certificate_requirements`
- `qualification_notes`
- `education_requirement`
- `major_requirement`
- `language_requirement`
- `experience_requirement`
- `salary_range`
- `jd`
- `requirements`
- `source_id`
- `source_url`
- `apply_url`
- `apply_method`
- `requires_login`
- `requires_captcha`
- `resume_format`
- `custom_questions_schema`
- `source_trust_level`
- `dedupe_key`
- `created_at`
- `updated_at`

### `job_snapshots`

Stores raw source state and historical versions.

Key fields:

- `id`
- `job_id`
- `source_id`
- `source_url`
- `content_hash`
- `raw_content`
- `parsed_payload`
- `attachment_paths`
- `captured_at`

### `manual_items`

Stores manual-only source records.

Key fields:

- `id`
- `platform`
- `company_name`
- `job_title`
- `search_keywords`
- `source_url`
- `tech_stack_keywords`
- `certificate_notes`
- `required_action`
- `requires_login`
- `requires_captcha`
- `apply_method`
- `reminder_at`
- `priority`
- `status`
- `linked_job_id`
- `notes`
- `created_at`
- `updated_at`

### `audit_logs`

Stores review and state-transition history.

Key fields:

- `id`
- `entity_type`
- `entity_id`
- `action`
- `before_payload`
- `after_payload`
- `notes`
- `created_at`

## Adapter Types

Initial adapter types:

- `GovNoticeAdapter`: government and SASAC HTML list/detail pages.
- `AttachmentExtractor`: Excel, Word, and PDF job tables.
- `OfficialCareerSiteAdapter`: official company career pages.
- `PublicATSAdapter`: Greenhouse and Lever first, Workday as configured support.
- `AggregatorIndexAdapter`: trusted index pages such as Nowcoder or 24365 for discovery.
- `SourceDirectoryAdapter`: SOE directories and company whitelist maintenance.
- `ManualImportAdapter`: user-supplied links, files, emails, or article pages.
- `ThirdPartyLeadAdapter`: lower-trust aggregators that only produce leads requiring official verification.

All adapters return normalized candidate records and metadata. They do not write directly to `jobs`.

## Scoring And Deduplication

Deduplication uses layered matching:

- Canonical source URL and content hash.
- Business key: normalized company, title, city, job type, batch, and deadline.
- Similarity hint for JD text, used only to propose merges.

Scoring inputs:

- Source trust.
- Recency.
- Deadline urgency.
- Role match.
- City match.
- Tech stack match.
- Certificate fit.
- Company category.
- Confidence penalty.
- Staleness penalty.

The MVP should keep scoring simple and visible. Users should understand why a job is high priority.

## Frontend Design

The app should open directly into the workbench, not a landing page.

Main views:

- Dashboard summary.
- Job table.
- Job detail.
- Review queue.
- Sources.
- Crawl tasks.
- Manual actions.

Frontend rules:

- Components should be small and reusable.
- Table columns and filters should be configuration-driven where practical.
- Detail page sections should be separate components.
- Avoid nested cards and heavy marketing layouts.
- Keep dense operational UI optimized for scanning.

## Backend Design

The backend should separate transport, domain logic, adapters, and persistence.

Recommended module boundaries:

- `api`: HTTP routes and request validation.
- `domain/jobs`: job normalization, scoring, status transitions.
- `domain/review`: review actions and audit logging.
- `domain/sources`: source policy and adapter orchestration.
- `adapters`: source-specific collection implementations.
- `persistence`: SQLite repositories and migrations.
- `tasks`: task runner and scheduling.
- `shared`: schema, result types, date and URL utilities.

Files should remain focused and preferably under 300 lines.

## Testing Strategy

Implementation must follow TDD.

Unit tests:

- Normalize adapter payloads into job fields.
- Parse tech stack and certificate requirements.
- Compute dedupe keys.
- Detect changed snapshots.
- Block manual-only, login-required, captcha-required, and forbidden sources.
- Approve, reject, and merge review items.
- Validate manual item required fields.

Integration tests:

- Crawl task creates review items for new jobs.
- Partial source failure does not fail the whole task.
- Review approval writes jobs and snapshots.
- Merge updates an existing job and logs the action.
- Manual item create/list/update works.
- SQLite persists after restart.
- Job filters, sorting, and pagination return expected records.

E2E tests:

- User triggers collection, reviews a job, approves it, and sees it in the table.
- User rejects an invalid review item and it does not appear in jobs.
- User creates a manual-only platform record and no automatic collection is triggered.
- User disables a source and collection skips it.
- User opens a job detail page and sees snapshots, tech stack, certificates, and application information.

## Documentation And Governance

During implementation, maintain:

- PRD.
- TRD.
- Data model.
- API contracts.
- Adapter specification.
- Test plan and test cases.
- Risk register.
- Change log.
- Architecture decision records.

Recommended structure:

```text
docs/
  prd/
  trd/
  qa/
  governance/
```

Subagent workflow:

- Documentation agent maintains PRD, TRD, test cases, risk records, and change logs.
- Implementation agents work from the written plan and TDD cases.
- Spec reviewers check requirement compliance.
- Code quality reviewers check maintainability, tests, and module boundaries.

## Risks

- Source pages may change structure.
- Some sources may hide roles in attachments.
- LLM extraction can hallucinate or over-normalize.
- Third-party aggregators can contain stale or unverified jobs.
- Strong anti-automation platforms can create legal, account, or reliability risk.
- SQLite is appropriate for local MVP but not shared multi-user deployment.

Mitigations:

- Preserve raw snapshots and attachments.
- Send low-confidence data to review.
- Keep source compliance status explicit.
- Keep adapters small and test-driven.
- Store manual-only records instead of automating restricted pages.
- Use migrations and clear backup paths for SQLite.

## Open Assumptions

- The first version is single-user and local-first.
- SQLite is the default persistence layer.
- The app will be implemented with a componentized frontend and a low-coupled backend.
- Batch resume submission is a future phase, so the MVP only stores application preparation fields.
- Manual-only sources can create reminders and notes but cannot trigger automated collection.

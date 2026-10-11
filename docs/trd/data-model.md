# Data Model

The MVP uses local SQLite with migrations and backup-friendly files.

## `sources`

Stores configured information sources.

Key fields: `id`, `name`, `type`, `base_url`, `list_url`, `adapter_type`, `mode`,
`compliance_status`, `trust_level`, `scope`, `enabled`, `cron`, `timezone`,
`min_interval_seconds`, `max_pages`, `max_items`, `selectors_json`,
`field_mapping_json`, `terms_url`, `terms_confirmed_at`, `robots_url`,
`timeout_seconds`, `last_run_at`, `last_status`, `created_at`, `updated_at`.

## `collection_runs`

Stores collection task runs.

Key fields: `id`, `source_id`, `trigger_type`, `status`, `started_at`, `finished_at`, `discovered_count`, `review_item_count`, `error_count`, `error_summary`.

## `agent_settings`

Stores one global optional Agent workflow configuration.

Key fields: `id`, `enabled`, `base_url`, `secret_env_ref`, `scan_model`,
`scan_reasoning_effort`, `process_model`, `process_reasoning_effort`,
`timeout_seconds`, `config_version`, `updated_at`. The provider key value is never
stored in SQLite.

## `review_items`

Stores candidate jobs or changes pending review.

Key fields: `id`, `source_id`, `run_id`, `candidate_payload`,
`normalized_payload`, `raw_snapshot_id`, `dedupe_key`, `content_hash`,
`confidence`, `status`, `review_notes`, `published_job_id`, `created_at`,
`updated_at`.

## `jobs`

Stores approved jobs.

Key fields: `id`, `company_type`, `company_name`, `business_unit`, `title`, `normalized_role`, `job_family`, `job_type`, `campus_batch`, `graduation_window`, `city`, `country_region`, `remote_policy`, `start_date`, `deadline`, `status`, `priority`, `tech_stack`, `certificate_requirements`, `qualification_notes`, `education_requirement`, `major_requirement`, `language_requirement`, `experience_requirement`, `salary_range`, `jd`, `requirements`, `source_id`, `source_url`, `apply_url`, `apply_method`, `requires_login`, `requires_captcha`, `resume_format`, `custom_questions_schema`, `source_trust_level`, `dedupe_key`, `created_at`, `updated_at`.

## `job_snapshots`

Stores raw source state and historical versions.

Key fields: `id`, `job_id`, `source_id`, `source_url`, `content_hash`, `raw_content`, `parsed_payload`, `attachment_paths`, `captured_at`.

## `manual_items`

Stores manual-only source records.

Key fields: `id`, `platform`, `company_name`, `job_title`, `search_keywords`, `source_url`, `tech_stack_keywords`, `certificate_notes`, `required_action`, `requires_login`, `requires_captcha`, `apply_method`, `reminder_at`, `priority`, `status`, `linked_job_id`, `notes`, `created_at`, `updated_at`.

## `audit_logs`

Stores review and state-transition history.

Key fields: `id`, `entity_type`, `entity_id`, `action`, `before_payload`, `after_payload`, `notes`, `created_at`.

## Incremental backend enhancements

V6 adds `jobs.source_row_id` and `jobs.official_job_id`. The existing
`business_unit` represents optional department and `campus_batch` stores the
optional graduation cohort exposed as integer `recruitmentYear`; absence stays
null. Review payloads and snapshots preserve original extraction evidence.

`browser_source_policies` stores explicit visible-browser approval tied to source
config version. `browser_tasks` stores row/character budgets,
current worker/lease token, lease expiry, status and accepted result hash. Task
completion is transactional and replay-safe; company rows become manual records
and independently evidenced roles become `PENDING_REVIEW` candidates.
`browser_source_pacing` reserves source-wide read times across worker processes;
`browser_ingestion_evidence` preserves immutable row evidence and unique replay
identity/content versions with links to the created pending records.

V7 adds `review_items.review_version` for competing review claims and
`job_projection_outbox` for SQL-triggered published-job insert/update/delete
events. Monotonic event IDs fence ES versions and form Redis cache generations.
Pending events remain durable until successful projection refresh. Redis and ES
are derived components; optional analytics has a separate synthetic dataset.

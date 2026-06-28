# Error Handling

## Source Policy Errors

The source policy returns explicit block reasons:

- `source_disabled`
- `manual_only`
- `login_required`
- `captcha_required`
- `forbidden`
- `unknown`

Blocked sources are skipped and recorded in task output. They do not invoke adapters. `source.compliance_status = unknown` is a blocked source state, not a review queue state.

## Adapter Errors

Adapter failures are captured per source with error summary and error count. A failed source does not stop other sources in the same collection run.

## Parsing Confidence

Low-confidence attachment parsing, unknown pages from known compliant sources, third-party aggregators, WeChat or article reposts, and LLM-extracted records enter review before approval.

## API Errors

Validation errors include field-level details. Policy errors include source policy reason. Unimplemented mutation endpoints can return typed unsupported-operation responses until domain services are available.

## Data Integrity

Raw snapshots and attachments are preserved when parsing succeeds or produces reviewable partial data. Review actions and state transitions create audit logs.

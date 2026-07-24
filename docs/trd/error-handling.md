# Error Handling

## Source Policy Errors

The source policy returns explicit block reasons:

- `source_disabled`
- `manual_only`
- `compliance_not_allowed`
- `terms_unconfirmed`
- `terms_url_missing`
- URL, origin, robots, address, login/captcha, and HTTP status reasons

Blocked sources are skipped and recorded in task output. They do not invoke adapters. `source.compliance_status = unknown` is a blocked source state, not a review queue state.

## Adapter Errors

Adapter failures are captured per source with error summary and error count. A failed source does not stop other sources in the same collection run.

## Parsing Confidence

Every new or changed candidate from a compliant source enters review.

## API Errors

Validation errors and policy failures use the common typed error envelope.

## Data Integrity

Candidate payloads and approved job snapshots are preserved. Review actions and
state transitions create audit logs.

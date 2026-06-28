# Risk Register

## Source Structure Drift

Risk: Source pages may change structure and break adapters.

Mitigation: Keep adapters small and test-driven, preserve raw snapshots, and route low-confidence parsing results to review.

## Attachment-Only Jobs

Risk: Some sources hide roles in Excel, Word, or PDF attachments.

Mitigation: Store attachment paths, parse official attachments, and send low-confidence extracted records to review.

## LLM Extraction Quality

Risk: LLM extraction can hallucinate or over-normalize fields.

Mitigation: Use LLM extraction only as assisted parsing and require review before approval.

## Third-Party Staleness

Risk: Third-party aggregators can contain stale or unverified jobs.

Mitigation: Treat aggregators as lower-trust leads and verify against official links before approval.

## Automation Compliance

Risk: Strong anti-automation platforms can create legal, account, or reliability risk.

Mitigation: Keep compliance status explicit and store manual-only records instead of automating restricted pages.

## Local Database Limits

Risk: SQLite is appropriate for a local MVP but not shared multi-user deployment.

Mitigation: Keep the first version single-user and local-first, use migrations, and maintain clear backup paths.

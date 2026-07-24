# E2E Scenarios

## Collection To Approval

The user triggers collection for an enabled allowed source, reviews a new candidate, approves it, and sees the job in the table with source trust, tech stack, certificates, deadline, and priority.

## Reject Invalid Candidate

The user rejects an invalid review item. The item remains auditable, and no approved job appears in the job table.

## Manual-Only Platform

The user creates a manual record for a login or captcha source. The record appears in manual actions, and no automatic collection task is triggered for that source.

## Disabled Source

The user disables a source. Collection skips it and records the skip reason without invoking an adapter.

## Job Detail Inspection

The user opens a job detail page and sees JD, requirements, tech stack,
certificate requirements, source snapshots, application method, login flag,
and captcha flag.

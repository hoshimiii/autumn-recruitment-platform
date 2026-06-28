# Test Cases

## Source Policy

- Enabled allowed auto adapter source returns allowed.
- Disabled source returns `source_disabled`.
- Manual-only source returns `manual_only`.
- Login-required source returns `login_required`.
- Captcha-required source returns `captcha_required`.
- Forbidden source returns `forbidden`.
- Unknown compliance source returns `unknown`.

## Job Normalization

- Company and title whitespace are trimmed.
- JD and requirements produce tech stack tags such as Java, Spring Boot, MySQL, Redis, Python, C++, React, Vue, and Linux.
- Certificate text identifies CET-4, CET-6, professional qualifications, computer rank certificates, soft exam certificates, party membership, driving license, security clearance, and work authorization or visa requirements.
- Certificate levels distinguish hard requirement, bonus, unknown, and not mentioned.

## Deduplication

- Canonical source URL and content hash identify exact repeats.
- Business key uses normalized company, title, city, job type, batch, and deadline.
- JD similarity proposes merges but does not silently merge records.

## Review

- Approve creates a job and snapshot.
- Merge updates existing job fields and logs before and after payloads.
- Reject prevents a review item from appearing in jobs.
- Needs manual follow-up creates or links a manual item.

## Manual Items

- Required fields validate platform, company or keywords, source URL, required action, login flag, captcha flag, and status.
- Manual item creation does not enqueue collection.
- Manual item update preserves linked job references.

## Persistence

- SQLite stores jobs, sources, tasks, review items, snapshots, manual items, and audit logs.
- Data persists after closing and reopening the database.

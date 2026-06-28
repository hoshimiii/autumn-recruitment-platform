# Product Requirements Document

## Goal

Build a local-first information integration platform for autumn recruitment jobs. The first version automatically discovers and normalizes jobs from approved sources, keeps risky sources as manual action records, and provides a clear table plus detail workflow for screening, reviewing, and preparing applications.

## Product Direction

The MVP is a whitelist source system with controlled one-shot or scheduled collection, structured parsing, deduplication, scoring, and a review queue. It is not a broad crawler.

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

## Primary Views

- Dashboard summary.
- Job table.
- Job detail.
- Review queue.
- Sources.
- Crawl tasks.
- Manual actions.

The app opens directly into the workbench. The interface is dense, operational, and optimized for scanning rather than marketing presentation.

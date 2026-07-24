# Compliance Boundary

## Allowed Automatic Collection

- Public government or SASAC notices.
- Public official career pages.
- Public ATS endpoints such as Greenhouse and Lever.
- Trusted public index pages when used only for discovery and official-link backtracking.

All automatic sources also require a traceable terms URL, confirmation time,
same-origin request boundary, and robots approval.

## Unknown Source Classification

`source.compliance_status = unknown` blocks automatic collection. A source must
be classified as `ALLOWED` before an adapter can run.

## Review-Required Results From Compliant Sources

- Third-party aggregators.
- WeChat or article reposts.
- Unknown pages discovered from a known compliant source.
- Lower-confidence configured extraction.

## Manual-Only Records

- Login-required pages.
- Captcha-required pages.
- Strong anti-bot or high-risk platforms.
- Platforms whose terms disallow automation.
- Attachment-only pages and AI extraction in this delivery.

For manual-only sources, the app stores platform name, company, job or search keywords, source URL, required manual action, reminder time, and notes. It does not run a crawler.

## Explicit Prohibitions

- Do not bypass login, captcha, rate limits, or anti-automation systems.
- Do not automatically scrape platforms whose terms prohibit automation.
- Do not use broad, unbounded internet crawling.
- Do not implement automatic resume submission in the MVP.

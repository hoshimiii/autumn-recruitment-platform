# Compliance Boundary

## Allowed Automatic Collection

- Public government or SASAC notices.
- Public official career pages.
- Public ATS endpoints such as Greenhouse and Lever.
- Public attachments linked from official notices.
- Trusted public index pages when used only for discovery and official-link backtracking.

## Review-Required Collection

- Third-party aggregators.
- WeChat or article reposts.
- Unknown pages.
- LLM-extracted records.
- Low-confidence attachment parsing.

## Manual-Only Records

- Login-required pages.
- Captcha-required pages.
- Strong anti-bot or high-risk platforms.
- Platforms whose terms disallow automation.

For manual-only sources, the app stores platform name, company, job or search keywords, source URL, required manual action, reminder time, and notes. It does not run a crawler.

## Explicit Prohibitions

- Do not bypass login, captcha, rate limits, or anti-automation systems.
- Do not automatically scrape platforms whose terms prohibit automation.
- Do not use broad, unbounded internet crawling.
- Do not implement automatic resume submission in the MVP.

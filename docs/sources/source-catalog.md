# Initial Source Catalog

Last researched: 2026-07-25.

This catalog is configuration guidance, not permission to collect. Every source
starts disabled until its current robots policy, terms, selectors, and request
limits are reviewed in the application.

## Trust Tiers

| Tier | Meaning | Publication rule |
|---|---|---|
| `OFFICIAL` | Company, government, or public-sector owner | Candidate still requires review |
| `TRUSTED_INDEX` | Government or university index linking to owner | Verify against owner URL |
| `LEAD_ONLY` | Third-party discovery lead | Manual verification only |
| `MANUAL` | Login, captcha, unclear terms, or unstable automation | No automated request |

## Seed Candidates

### Tencent Careers JSON

- Trust: `OFFICIAL`
- Category: `INTERNET_BIG_TECH`
- Mode: `JSON`
- Base URL: `https://careers.tencent.com`
- List URL:
  `https://careers.tencent.com/tencentcareer/api/post/Query?pageIndex=1&pageSize=20&language=zh-cn&area=cn&attrId=2,3`
- Verified response: anonymous HTTP 200 JSON on 2026-07-25.
- Items path: `Data.Posts`
- Field mapping:
  - external ID: `PostId`
  - title: `RecruitPostName`
  - city: `LocationName`
  - family: `CategoryName`
  - JD: `Responsibility`
  - requirements: `Requirement`
  - updated date: `LastUpdateTime`
  - source/apply URL: derive HTTPS job URL from `PostId`
- Limits: one page and at most 20 items per test run; daily maximum is configured.
- Caveat: `attrId` semantics are not an official public contract. Treat changes
  as parse failures and require operator review.

### State-owned Assets Supervision Recruitment Index

- Trust: `OFFICIAL`
- Category: `CENTRAL_SOE`
- Mode: `HTML`
- URL: `https://www.sasac.gov.cn/n2588035/n2588325/n2588350/index.html`
- Content: central enterprise recruitment notices with dates and detail links.
- Extraction baseline: bounded links whose text contains recruitment keywords.
- Caveat: local TLS fetch was not verified in the current shell. Keep disabled
  until the source test succeeds and policy review is recorded.

### Shandong Rural Credit Recruitment

- Trust: `OFFICIAL`
- Category: `SHANDONG_SOE`
- Mode: `HTML`
- URL: `https://www.sdrcu.com/sdnxs/374215/374221/index.html`
- Content: official recruitment notices and PDF attachments.
- Extraction baseline: bounded recruitment notice links; attachments remain
  review evidence until a dedicated attachment adapter exists.
- Caveat: current catalog evidence includes a 2026 graduate recruitment notice,
  but selectors and policy must be tested before enabling.

### Alibaba Campus Recruitment

- Trust: `OFFICIAL`
- Category: `INTERNET_BIG_TECH`
- Mode: `HTML` or `MANUAL`
- URL: `https://campus-talent.alibaba.com/`
- Content: recruitment batches, graduate windows, and role families.
- Caveat: use public landing information only. If detailed positions require
  authentication or an unstable client API, create manual records instead.

### Huawei Campus Recruitment

- Trust: `OFFICIAL`
- Category: `INTERNET_BIG_TECH`
- Mode: `HTML` or `MANUAL`
- URL: `https://career.huawei.com/cn/campus-recruitment`
- Content: graduate windows, programs, and public recruitment updates.
- Caveat: do not automate application/login pages.

## Discovery Only

Government recruitment indexes, local state-owned asset supervision pages, and
university employment centers may discover leads. A lead is not published until
the operator confirms the company-owned or government-owned source URL.

Third-party recruitment platforms and social media are manual-only whenever they
require login, captcha, browser fingerprinting, or prohibit automated access.

## Operational Review Checklist

Before enabling a source:

1. Record owner, trust tier, base origin, and source category.
2. Fetch and review `robots.txt`; record the review time and result.
3. Record the terms URL and operator confirmation time.
4. Test with one page and a small item limit.
5. Verify every candidate has an original URL and capture time.
6. Confirm `401`, `403`, `429`, login redirects, and captcha indicators stop work.
7. Set a minimum interval of at least five seconds unless the source publishes a
   stricter rule.
8. Enable the source only after the test run is visible and reviewable.

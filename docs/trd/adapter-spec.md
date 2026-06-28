# Adapter Specification

## Adapter Types

- `GovNoticeAdapter`: government and SASAC HTML list/detail pages.
- `AttachmentExtractor`: Excel, Word, and PDF job tables.
- `OfficialCareerSiteAdapter`: official company career pages.
- `PublicATSAdapter`: Greenhouse and Lever first, Workday as configured support.
- `AggregatorIndexAdapter`: trusted index pages such as Nowcoder or 24365 for discovery.
- `SourceDirectoryAdapter`: SOE directories and company whitelist maintenance.
- `ManualImportAdapter`: user-supplied links, files, emails, or article pages.
- `ThirdPartyLeadAdapter`: lower-trust aggregators that only produce leads requiring official verification.

## Adapter Contract

Adapters receive source configuration and task context. They return normalized candidate records plus metadata, raw snapshot references, attachment paths, confidence, source URL, content hash, and parser warnings.

Adapters do not write directly to `jobs`. They create candidates for review or high-confidence direct processing through domain services.

## Discovery And Parsing Rules

- Official public pages and public ATS APIs may be collected automatically when source policy allows it.
- Public attachments linked from official notices may be downloaded and parsed.
- Trusted public index pages may be used only for discovery and official-link backtracking.
- LLM extraction is an assisted parser for long notices or non-standard pages and sends results to review.
- Third-party leads require official verification before approval.

## Dedupe And Scoring Inputs

Deduplication uses canonical source URL and content hash, business key of normalized company, title, city, job type, batch, and deadline, and a JD similarity hint used only to propose merges.

Scoring uses source trust, recency, deadline urgency, role match, city match, tech stack match, certificate fit, company category, confidence penalty, and staleness penalty.

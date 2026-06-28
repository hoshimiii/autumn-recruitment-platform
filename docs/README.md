# Autumn Recruitment Platform Docs

This documentation set tracks the product, technical, testing, and governance records for the local-first autumn recruitment platform.

Start here:

- Product: `prd/PRD.md`
- Technical design: `trd/TRD.md`
- Data model: `trd/data-model.md`
- Test plan: `qa/test-plan.md`
- Risks: `governance/risk-register.md`
- Decisions: `governance/decision-records/`

## Workspace Baseline

The root package is an npm workspace for future `apps/*` and `packages/*` projects. Bootstrap scripts that fan out to workspaces first check for workspace package manifests, then use `--if-present`, so documentation-only Task 1 remains usable before workspace packages are created.

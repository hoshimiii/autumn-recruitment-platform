export type CompanyType =
  | "central_soe"
  | "provincial_soe"
  | "municipal_soe"
  | "internet_big_tech"
  | "sme"
  | "foreign_company"
  | "public_institution"
  | "unknown";

export type JobType =
  | "campus"
  | "internship"
  | "social"
  | "early_batch"
  | "autumn_recruitment"
  | "spring_recruitment";

export type JobStatus = "new" | "tracking" | "applied" | "closed" | "ignored";

export type SourceTrust = "official" | "trusted_index" | "third_party_lead" | "manual" | "unknown";

export type TechStackTag =
  | "frontend"
  | "backend"
  | "fullstack"
  | "mobile"
  | "data"
  | "ai"
  | "cloud"
  | "devops"
  | "security"
  | "embedded"
  | "unknown";

export type CertificateRequirementLevel = "required" | "bonus" | "unknown" | "not_mentioned";

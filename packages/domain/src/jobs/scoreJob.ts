import type { CertificateRequirement } from "./normalizeJob.js";
import type { SourceTrust } from "./jobTypes.js";

export type DeadlineStatus = "open" | "closing_soon" | "closed" | "unknown";

export type ScoreJobInput = {
  sourceTrustLevel: SourceTrust;
  techStack: string[];
  certificateRequirements: CertificateRequirement[];
  userTechStack: string[];
  userCertificates: string[];
  deadlineStatus: DeadlineStatus;
};

const BASE_SCORE = 20;
const MAX_SOURCE_SCORE = 30;
const MAX_DEADLINE_SCORE = 15;
const MAX_TECH_SCORE = 25;
const MAX_CERTIFICATE_SCORE = 10;

const sourceTrustScores: Record<SourceTrust, number> = {
  official: MAX_SOURCE_SCORE,
  trusted_index: 24,
  manual: 18,
  third_party_lead: 12,
  unknown: 6
};

const deadlineScores: Record<DeadlineStatus, number> = {
  open: MAX_DEADLINE_SCORE,
  closing_soon: 10,
  unknown: 5,
  closed: 0
};

export function scoreJob(input: ScoreJobInput): number {
  const score =
    BASE_SCORE +
    sourceTrustScores[input.sourceTrustLevel] +
    deadlineScores[input.deadlineStatus] +
    scoreTechMatch(input.techStack, input.userTechStack) +
    scoreCertificateFit(input.certificateRequirements, input.userCertificates);

  return boundScore(Math.round(score));
}

function scoreTechMatch(jobTechStack: string[], userTechStack: string[]): number {
  if (jobTechStack.length === 0) {
    return 0;
  }

  const normalizedUserTech = new Set(userTechStack.map(normalizeToken));
  const matchedCount = jobTechStack
    .map(normalizeToken)
    .filter((tech) => normalizedUserTech.has(tech)).length;

  return (matchedCount / jobTechStack.length) * MAX_TECH_SCORE;
}

function scoreCertificateFit(
  certificateRequirements: CertificateRequirement[],
  userCertificates: string[]
): number {
  const relevantRequirements = certificateRequirements.filter(
    (requirement) => requirement.level === "required" || requirement.level === "bonus"
  );

  if (relevantRequirements.length === 0) {
    return 0;
  }

  const normalizedUserCertificates = new Set(userCertificates.map(normalizeToken));
  const matchedCount = relevantRequirements
    .map((requirement) => normalizeToken(requirement.name))
    .filter((certificateName) => normalizedUserCertificates.has(certificateName)).length;

  return (matchedCount / relevantRequirements.length) * MAX_CERTIFICATE_SCORE;
}

function normalizeToken(value: string): string {
  return value.trim().toLowerCase();
}

function boundScore(score: number): number {
  return Math.max(0, Math.min(100, score));
}

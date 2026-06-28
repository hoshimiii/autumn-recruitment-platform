import type { CertificateRequirementLevel } from "./jobTypes.js";

export type { CertificateRequirementLevel } from "./jobTypes.js";

export type CertificateRequirement = {
  name: string;
  level: CertificateRequirementLevel;
};

export type JobCandidateInput = {
  companyName: string;
  title: string;
  companyType: string;
  jobType: string;
  city: string;
  jd: string;
  requirements: string;
  sourceUrl: string;
  applyUrl?: string;
  deadline?: string;
};

export type NormalizedJob = JobCandidateInput & {
  techStack: string[];
  certificateRequirements: CertificateRequirement[];
};

const techPatterns: Array<[string, RegExp]> = [
  ["Java", /\bJava\b/i],
  ["Spring Boot", /\bSpring\s*Boot\b/i],
  ["MySQL", /\bMySQL\b/i],
  ["Redis", /\bRedis\b/i],
  ["Python", /\bPython\b/i],
  ["C++", /C\+\+/i],
  ["React", /\bReact\b/i],
  ["Vue", /\bVue\b/i],
  ["Linux", /\bLinux\b/i]
];

export function normalizeJobCandidate(input: JobCandidateInput): NormalizedJob {
  const searchableText = `${input.jd} ${input.requirements}`;

  return {
    ...input,
    companyName: input.companyName.trim(),
    title: input.title.trim(),
    city: input.city.trim(),
    techStack: detectTechStack(searchableText),
    certificateRequirements: detectCertificateRequirements(searchableText)
  };
}

function detectTechStack(text: string): string[] {
  return techPatterns
    .filter(([, pattern]) => pattern.test(text))
    .map(([name]) => name);
}

function detectCertificateRequirements(text: string): CertificateRequirement[] {
  if (!/(英语四级|CET-?4)/i.test(text)) {
    return [];
  }

  const level: CertificateRequirementLevel = /(优先|加分)/.test(text) ? "bonus" : "unknown";

  return [{ name: "CET-4", level }];
}

import type { CertificateRequirementLevel, CompanyType, JobType } from "./jobTypes.js";

export type { CertificateRequirementLevel } from "./jobTypes.js";

export type CertificateRequirement = {
  name: string;
  level: CertificateRequirementLevel;
};

export type JobCandidateInput = {
  companyName: string;
  title: string;
  companyType: CompanyType;
  jobType: JobType;
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

  const level: CertificateRequirementLevel = detectCET4RequirementLevel(text);

  return [{ name: "CET-4", level }];
}

function detectCET4RequirementLevel(text: string): CertificateRequirementLevel {
  if (hasRequiredCET4Wording(text)) {
    return "required";
  }

  if (/(优先|加分)/.test(text)) {
    return "bonus";
  }

  return "unknown";
}

function hasRequiredCET4Wording(text: string): boolean {
  return [
    /(必备|要求).{0,12}(英语四级|CET-?4)/i,
    /(英语四级|CET-?4).{0,12}(必备|要求)/i,
    /(需通过|通过).{0,12}(英语四级|CET-?4)/i,
    /(英语四级|CET-?4).{0,12}(required)/i,
    /(required).{0,12}(英语四级|CET-?4)/i
  ].some((pattern) => pattern.test(text));
}

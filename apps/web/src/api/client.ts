export type JobStatus = "待评估" | "待投递" | "已投递" | "需人工处理";

import type { ManualActionStatus, RequiredManualAction, SourceTrust } from "@recruitment/domain";

export type { ManualActionStatus, RequiredManualAction, SourceTrust } from "@recruitment/domain";

export type ManualActionInput = {
  platform: string;
  companyName: string;
  jobTitle?: string;
  requiredAction: RequiredManualAction;
  searchKeywords?: string[];
  sourceUrl?: string;
  techStackKeywords?: string[];
  certificateNotes?: string;
  applyMethod?: string;
  reminderAt?: string;
  requiresLogin?: boolean;
  requiresCaptcha?: boolean;
  priority?: number;
  status?: ManualActionStatus;
  notes?: string;
};

export type Job = {
  id: string;
  sourceName: string;
  sourceType: string;
  sourceUrl: string;
  sourceConfidence: number;
  sourceRequiresLogin: boolean;
  sourceRequiresCaptcha: boolean;
  sourceTrust: SourceTrust;
  manualActionStatus: ManualActionStatus;
  requiredAction?: RequiredManualAction;
  companyType: string;
  companyName: string;
  title: string;
  jobFamily: string;
  jobType: string;
  city: string;
  techStack: string[];
  certificateRequirements: string[];
  deadline: string;
  deadlineStatus: string;
  applicationStatus: string;
  reviewStatus: string;
  priority: number;
  status: JobStatus;
  description: string;
  requirements: string[];
  snapshots: string[];
  applicationInfo: string;
};

export type JobFilters = {
  keyword: string;
  companyType: string;
  companyName: string;
  jobFamily: string;
  jobType: string;
  city: string;
  sourceType: string;
  status: string;
  techStack: string;
  certificate: string;
  deadlineStatus: string;
  sourceTrust: string;
  applicationStatus: string;
  reviewStatus: string;
  manualState: string;
};

export type ReviewItem = {
  id: string;
  title: string;
  company: string;
  reason: string;
  priority: "高" | "中" | "低";
};

export type Source = {
  id: string;
  name: string;
  type: string;
  status: string;
  lastSync: string;
  needsLogin: boolean;
  needsCaptcha: boolean;
};

export type Task = {
  id: string;
  title: string;
  owner: string;
  due: string;
  state: string;
};

export type WorkbenchData = {
  jobs: Job[];
  reviewQueue: ReviewItem[];
  sources: Source[];
  tasks: Task[];
};

export const emptyJobFilters: JobFilters = {
  keyword: "",
  companyType: "",
  companyName: "",
  jobFamily: "",
  jobType: "",
  city: "",
  sourceType: "",
  status: "",
  techStack: "",
  certificate: "",
  deadlineStatus: "",
  sourceTrust: "",
  applicationStatus: "",
  reviewStatus: "",
  manualState: ""
};

export function filterJobs(jobs: Job[], filters: JobFilters): Job[] {
  const keyword = filters.keyword.trim().toLowerCase();

  return jobs.filter((job) => {
    const matchesKeyword =
      !keyword ||
      [job.companyName, job.title, job.city].some((value) => value.toLowerCase().includes(keyword));
    const matchesJobType = !filters.jobType || job.jobType === filters.jobType;
    const matchesCompanyType = !filters.companyType || job.companyType === filters.companyType;
    const matchesCompanyName = !filters.companyName || job.companyName === filters.companyName;
    const matchesJobFamily = !filters.jobFamily || job.jobFamily === filters.jobFamily;
    const matchesCity = !filters.city || job.city === filters.city;
    const matchesSourceType = !filters.sourceType || job.sourceType === filters.sourceType;
    const matchesStatus = !filters.status || job.status === filters.status;
    const matchesTech = !filters.techStack || job.techStack.includes(filters.techStack);
    const matchesCertificate =
      !filters.certificate || job.certificateRequirements.includes(filters.certificate);
    const matchesDeadline = !filters.deadlineStatus || job.deadlineStatus === filters.deadlineStatus;
    const matchesSourceTrust = !filters.sourceTrust || job.sourceTrust === filters.sourceTrust;
    const matchesApplication = !filters.applicationStatus || job.applicationStatus === filters.applicationStatus;
    const matchesReview = !filters.reviewStatus || job.reviewStatus === filters.reviewStatus;
    const matchesManual = !filters.manualState || job.manualActionStatus === filters.manualState;

    return (
      matchesKeyword &&
      matchesCompanyType &&
      matchesCompanyName &&
      matchesJobFamily &&
      matchesJobType &&
      matchesCity &&
      matchesSourceType &&
      matchesStatus &&
      matchesTech &&
      matchesCertificate &&
      matchesDeadline &&
      matchesSourceTrust &&
      matchesApplication &&
      matchesReview &&
      matchesManual
    );
  });
}

export function uniqueValues(values: string[]): string[] {
  return Array.from(new Set(values)).sort((a, b) => a.localeCompare(b, "zh-CN"));
}

import type { Job, ReviewItem } from "../api/client";
import { JobTable } from "../features/jobs/JobTable";
import { PageHeader } from "../features/layout/PageHeader";
import { ReviewQueue } from "../features/review/ReviewQueue";

type OpsReviewPageProps = {
  jobs: Job[];
  onSelectJob: (jobId: string) => void;
  reviewQueue: ReviewItem[];
  selectedJobId: string;
};

export function OpsReviewPage({ jobs, onSelectJob, reviewQueue, selectedJobId }: OpsReviewPageProps) {
  const pendingReviewJobs = jobs.filter((job) => job.reviewStatus === "待复核");

  return (
    <main className="workbench">
      <PageHeader
        title="岗位复核"
        description="处理高优先级岗位、来源可信度和候选人条件匹配的人工判断。"
      />

      <div className="ops-grid">
        <ReviewQueue items={reviewQueue} />
        <JobTable jobs={pendingReviewJobs} selectedJobId={selectedJobId} onSelectJob={onSelectJob} />
      </div>
    </main>
  );
}

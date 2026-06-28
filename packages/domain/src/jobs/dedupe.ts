export type JobDedupeInput = {
  companyName: string;
  title: string;
  city: string;
  jobType: string;
  deadline?: string;
};

export function buildJobDedupeKey(input: JobDedupeInput): string {
  return [
    input.companyName,
    input.title,
    input.city,
    input.jobType,
    input.deadline ?? ""
  ].map((value) => value.trim()).join("|");
}

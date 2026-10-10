package dev.recruitops.web;

import dev.recruitops.infrastructure.JobRepository;

final class JobFilters {
  private JobFilters() {}

  static JobRepository.JobFilter of(String keyword, String companyCategory, String company, String jobType, String city,
      String techStack, String certificate, String deadlineFrom, String deadlineTo, String sourceTrust, String changedSince,
      int page, int pageSize, String businessUnit, String title, String recruitmentYear) {
    return new JobRepository.JobFilter(keyword, companyCategory, company, jobType, city, techStack, certificate,
        deadlineFrom, deadlineTo, sourceTrust, changedSince, page, pageSize, businessUnit, title, recruitmentYear);
  }
}

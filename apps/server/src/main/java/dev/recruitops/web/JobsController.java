package dev.recruitops.web;

import dev.recruitops.application.JobService;
import dev.recruitops.infrastructure.JobRepository;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/jobs")
public class JobsController {
  private final JobService jobs;
  public JobsController(JobService jobs) { this.jobs = jobs; }
  @GetMapping public JobRepository.Page list(@RequestParam(required=false) String keyword, @RequestParam(required=false) String companyCategory,
      @RequestParam(required=false) String company, @RequestParam(required=false) String jobType, @RequestParam(required=false) String city,
      @RequestParam(required=false) String techStack, @RequestParam(required=false) String certificate, @RequestParam(required=false) String deadlineFrom,
      @RequestParam(required=false) String deadlineTo, @RequestParam(required=false) String sourceTrust, @RequestParam(required=false) String changedSince,
      @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize) {
    return jobs.list(JobFilters.of(keyword, companyCategory, company, jobType, city, techStack, certificate, deadlineFrom, deadlineTo, sourceTrust, changedSince, page, pageSize));
  }
  @GetMapping("/{id}") public Map<String, Object> detail(@PathVariable long id) {
    return jobs.detail(id);
  }
}

package dev.recruitops.web;

import dev.recruitops.application.JobExportService;
import dev.recruitops.infrastructure.JobRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exports")
public class ExportsController {
  private final JobExportService exports;
  public ExportsController(JobExportService exports) { this.exports = exports; }

  @GetMapping(value = "/jobs.md", produces = "text/markdown;charset=UTF-8")
  public ResponseEntity<String> markdown(@RequestParam(required=false) String keyword, @RequestParam(required=false) String companyCategory,
      @RequestParam(required=false) String company, @RequestParam(required=false) String jobType, @RequestParam(required=false) String city,
      @RequestParam(required=false) String techStack, @RequestParam(required=false) String certificate, @RequestParam(required=false) String deadlineFrom,
      @RequestParam(required=false) String deadlineTo, @RequestParam(required=false) String sourceTrust, @RequestParam(required=false) String changedSince,
      @RequestParam(required=false) String businessUnit, @RequestParam(required=false) String title, @RequestParam(required=false) String recruitmentYear) {
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=jobs.md")
        .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8)).body(exports.markdown(filter(keyword, companyCategory, company, jobType, city, techStack, certificate, deadlineFrom, deadlineTo, sourceTrust, changedSince, businessUnit, title, recruitmentYear)));
  }

  @GetMapping(value = "/jobs.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> xlsx(@RequestParam(required=false) String keyword, @RequestParam(required=false) String companyCategory,
      @RequestParam(required=false) String company, @RequestParam(required=false) String jobType, @RequestParam(required=false) String city,
      @RequestParam(required=false) String techStack, @RequestParam(required=false) String certificate, @RequestParam(required=false) String deadlineFrom,
      @RequestParam(required=false) String deadlineTo, @RequestParam(required=false) String sourceTrust, @RequestParam(required=false) String changedSince,
      @RequestParam(required=false) String businessUnit, @RequestParam(required=false) String title, @RequestParam(required=false) String recruitmentYear) {
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=jobs.xlsx")
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(exports.xlsx(filter(keyword, companyCategory, company, jobType, city, techStack, certificate, deadlineFrom, deadlineTo, sourceTrust, changedSince, businessUnit, title, recruitmentYear)));
  }

  private JobRepository.JobFilter filter(String keyword, String companyCategory, String company, String jobType, String city,
      String techStack, String certificate, String deadlineFrom, String deadlineTo, String sourceTrust, String changedSince,
      String businessUnit, String title, String recruitmentYear) {
    return JobFilters.of(keyword, companyCategory, company, jobType, city, techStack, certificate, deadlineFrom, deadlineTo, sourceTrust, changedSince, 1, 100, businessUnit, title, recruitmentYear);
  }
}

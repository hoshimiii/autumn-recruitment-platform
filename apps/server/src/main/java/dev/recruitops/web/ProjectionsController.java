package dev.recruitops.web;

import dev.recruitops.application.JobProjectionService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/projections")
public class ProjectionsController {
  private final JobProjectionService projections;
  public ProjectionsController(JobProjectionService projections) { this.projections=projections; }
  @GetMapping public Map<String,Object> status() { return projections.status(); }
  @PostMapping("/rebuild") public Map<String,Integer> rebuild() { return Map.of("enqueued",projections.rebuild()); }
}

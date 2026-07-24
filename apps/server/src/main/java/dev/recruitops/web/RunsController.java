package dev.recruitops.web;

import dev.recruitops.domain.CollectionRun;
import dev.recruitops.infrastructure.RunRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/runs")
public class RunsController {
  private final RunRepository runs;
  public RunsController(RunRepository runs) { this.runs = runs; }
  @GetMapping public java.util.Map<String, List<CollectionRun>> list() { return java.util.Map.of("items", runs.list()); }
}

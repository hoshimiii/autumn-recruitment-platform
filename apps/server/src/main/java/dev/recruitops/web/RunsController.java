package dev.recruitops.web;

import dev.recruitops.application.CollectionWorkflow;
import dev.recruitops.domain.CollectionRun;
import dev.recruitops.infrastructure.RunRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/runs")
public class RunsController {
  private final RunRepository runs;
  private final CollectionWorkflow workflow;
  public RunsController(RunRepository runs, CollectionWorkflow workflow) {
    this.runs = runs;
    this.workflow = workflow;
  }
  @GetMapping public java.util.Map<String, List<CollectionRun>> list() { return java.util.Map.of("items", runs.list()); }
  @PostMapping("/daily") public CollectionRun daily() { return workflow.runDaily(); }
}

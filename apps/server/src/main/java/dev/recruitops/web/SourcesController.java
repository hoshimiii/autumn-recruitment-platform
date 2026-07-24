package dev.recruitops.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import dev.recruitops.application.CollectionWorkflow;
import dev.recruitops.application.SourceService;
import dev.recruitops.domain.CollectionRun;
import dev.recruitops.domain.Source;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/sources")
public class SourcesController {
  private final SourceService sources;
  private final CollectionWorkflow workflow;
  public SourcesController(SourceService sources, CollectionWorkflow workflow) {
    this.sources = sources;
    this.workflow = workflow;
  }
  @GetMapping public java.util.Map<String, List<Source>> list() { return java.util.Map.of("items", sources.list()); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public Source create(@Valid @RequestBody SourceRequest request) { return sources.create(request.toInput()); }
  @PutMapping("/{id}") public Source update(@PathVariable long id, @Valid @RequestBody SourceRequest request) { return sources.update(id, request.toInput()); }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable long id) { sources.delete(id); }
  @PostMapping("/{id}/test") public CollectionRun test(@PathVariable long id) {
    return workflow.testSource(id);
  }
  @PostMapping("/{id}/runs") public CollectionRun run(@PathVariable long id) {
    return workflow.runSource(id);
  }
  public record SourceRequest(
      @NotBlank String name, @NotBlank String type, @NotBlank String baseUrl, String listUrl,
      @NotBlank String adapterType, @NotBlank String mode, @NotBlank String complianceStatus,
      @NotBlank String trustLevel, @NotBlank String companyCategory, Boolean enabled, String cron,
      String timezone, Integer minIntervalSeconds, Integer maxPages, Integer maxItems, String selectors,
      String fieldMapping, String termsUrl, String termsConfirmedAt, String robotsUrl, Integer timeoutSeconds,
      Boolean aiEnabled, String aiProvider, String aiModel,
      @JsonAlias("aiSecretRef") String secretEnvRef) {
    SourceService.SourceInput toInput() {
      return new SourceService.SourceInput(name, type, baseUrl, listUrl, adapterType, mode,
          complianceStatus, trustLevel, companyCategory, enabled, cron, timezone, minIntervalSeconds,
          maxPages, maxItems, selectors, fieldMapping, termsUrl, termsConfirmedAt, robotsUrl,
          timeoutSeconds, aiEnabled, aiProvider, aiModel, secretEnvRef);
    }
  }
}

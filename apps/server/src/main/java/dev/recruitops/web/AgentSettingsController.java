package dev.recruitops.web;

import dev.recruitops.application.AgentSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent-settings")
public class AgentSettingsController {
  private final AgentSettingsService settings;

  public AgentSettingsController(AgentSettingsService settings) { this.settings = settings; }

  @GetMapping public AgentSettingsService.SettingsResponse get() { return settings.get(); }
  @PutMapping public AgentSettingsService.SettingsResponse update(@RequestBody AgentSettingsService.SettingsInput input) {
    return settings.update(input);
  }
  @PostMapping("/check") public AgentSettingsService.CatalogCheck check() { return settings.check(); }
}

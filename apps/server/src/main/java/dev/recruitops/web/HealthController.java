package dev.recruitops.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/health")
public class HealthController {
  private final JdbcTemplate jdbc;
  public HealthController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  @GetMapping public java.util.Map<String, String> health() {
    jdbc.queryForObject("SELECT 1", Integer.class); return java.util.Map.of("status", "UP", "database", "UP", "scheduler", "NOT_CONFIGURED");
  }
}

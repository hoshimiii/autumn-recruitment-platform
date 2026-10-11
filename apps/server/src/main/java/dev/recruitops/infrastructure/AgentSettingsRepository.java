package dev.recruitops.infrastructure;

import dev.recruitops.domain.AgentSettings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AgentSettingsRepository {
  private final JdbcTemplate jdbc;

  public AgentSettingsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public AgentSettings get() {
    return jdbc.queryForObject("SELECT * FROM agent_settings WHERE id=1", (rs, row) -> new AgentSettings(
        rs.getLong("id"), rs.getBoolean("enabled"), rs.getString("base_url"),
        rs.getString("secret_env_ref"), rs.getString("scan_model"),
        rs.getString("scan_reasoning_effort"), rs.getString("process_model"),
        rs.getString("process_reasoning_effort"), rs.getInt("timeout_seconds"),
        rs.getInt("config_version"), rs.getString("updated_at")));
  }

  public AgentSettings update(AgentSettings settings) {
    jdbc.update("""
        UPDATE agent_settings SET enabled=?,base_url=?,secret_env_ref=?,scan_model=?,scan_reasoning_effort=?,
        process_model=?,process_reasoning_effort=?,timeout_seconds=?,config_version=config_version+1,
        updated_at=CURRENT_TIMESTAMP WHERE id=1
        """, settings.enabled(), settings.baseUrl(), settings.secretEnvRef(), settings.scanModel(),
        settings.scanReasoningEffort(), settings.processModel(), settings.processReasoningEffort(),
        settings.timeoutSeconds());
    return get();
  }
}

package dev.recruitops.infrastructure;

import dev.recruitops.domain.Source;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SourceRepository {
  private final JdbcTemplate jdbc;
  private final RowMapper<Source> mapper = (rs, row) -> new Source(rs.getLong("id"), rs.getString("name"),
      rs.getString("type"), rs.getString("base_url"), rs.getString("list_url"), rs.getString("adapter_type"),
      rs.getString("mode"), rs.getString("compliance_status"), rs.getString("trust_level"),
      rs.getString("scope"), rs.getBoolean("enabled"), rs.getString("cron"), rs.getString("timezone"),
      rs.getInt("min_interval_seconds"), rs.getInt("max_pages"), rs.getInt("max_items"),
      rs.getString("selectors_json"), rs.getString("field_mapping_json"), rs.getString("terms_url"),
      rs.getString("terms_confirmed_at"), rs.getString("robots_url"), rs.getInt("timeout_seconds"),
      rs.getString("etag"), rs.getString("last_modified"), rs.getString("last_run_at"),
      rs.getString("last_status"), rs.getBoolean("ai_enabled"), rs.getString("ai_provider"),
      rs.getString("ai_model"), rs.getInt("config_version"));
  public SourceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public List<Source> list() { return jdbc.query("SELECT * FROM sources ORDER BY id DESC", mapper); }
  public List<Source> listEnabled() {
    return jdbc.query("SELECT * FROM sources WHERE enabled=1 ORDER BY id", mapper);
  }
  public Optional<Source> find(long id) { return jdbc.query("SELECT * FROM sources WHERE id=?", mapper, id).stream().findFirst(); }
  public void recordRun(long id, String status, String etag, String lastModified) {
    jdbc.update("""
        UPDATE sources SET last_run_at=CURRENT_TIMESTAMP,last_status=?,
        etag=COALESCE(?,etag),last_modified=COALESCE(?,last_modified),updated_at=CURRENT_TIMESTAMP
        WHERE id=?
        """, status, etag, lastModified, id);
  }
  @org.springframework.transaction.annotation.Transactional
  public Source create(Source source, String secretEnvRef) {
    jdbc.update("""
        INSERT INTO sources (name,type,base_url,list_url,adapter_type,mode,compliance_status,trust_level,
        scope,enabled,secret_env_ref,cron,timezone,min_interval_seconds,max_pages,max_items,selectors_json,
        field_mapping_json,terms_url,terms_confirmed_at,robots_url,timeout_seconds,ai_enabled,ai_provider,ai_model)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
        source.name(), source.type(), source.baseUrl(), source.listUrl(), source.adapterType(), source.mode(),
        source.complianceStatus(), source.trustLevel(), source.companyCategory(), source.enabled(), secretEnvRef,
        source.cron(), source.timezone(), source.minIntervalSeconds(), source.maxPages(), source.maxItems(),
        source.selectors(), source.fieldMapping(), source.termsUrl(), source.termsConfirmedAt(),
        source.robotsUrl(), source.timeoutSeconds(), source.aiEnabled(), source.aiProvider(), source.aiModel());
    return find(jdbc.queryForObject("SELECT last_insert_rowid()", Long.class)).orElseThrow();
  }
  public Source update(long id, Source source, String secretEnvRef) {
    int changed = jdbc.update("""
        UPDATE sources SET name=?,type=?,base_url=?,list_url=?,adapter_type=?,mode=?,compliance_status=?,
        trust_level=?,scope=?,enabled=?,secret_env_ref=COALESCE(?,secret_env_ref),cron=?,timezone=?,
        min_interval_seconds=?,max_pages=?,max_items=?,selectors_json=?,field_mapping_json=?,terms_url=?,
        terms_confirmed_at=?,robots_url=?,timeout_seconds=?,ai_enabled=?,ai_provider=?,ai_model=?,
        config_version=config_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?""",
        source.name(), source.type(), source.baseUrl(), source.listUrl(), source.adapterType(), source.mode(),
        source.complianceStatus(), source.trustLevel(), source.companyCategory(), source.enabled(), secretEnvRef,
        source.cron(), source.timezone(), source.minIntervalSeconds(), source.maxPages(), source.maxItems(),
        source.selectors(), source.fieldMapping(), source.termsUrl(), source.termsConfirmedAt(),
        source.robotsUrl(), source.timeoutSeconds(), source.aiEnabled(), source.aiProvider(), source.aiModel(), id);
    if (changed == 0) throw new MissingResourceException("Source not found");
    return find(id).orElseThrow();
  }
  public void delete(long id) {
    if (jdbc.update("DELETE FROM sources WHERE id=?", id) == 0) throw new MissingResourceException("Source not found");
  }
}

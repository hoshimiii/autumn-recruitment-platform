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
      rs.getString("type"), rs.getString("base_url"), rs.getString("adapter_type"), rs.getString("mode"),
      rs.getString("compliance_status"), rs.getString("trust_level"), rs.getString("scope"),
      rs.getBoolean("enabled"), rs.getInt("config_version"));
  public SourceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public List<Source> list() { return jdbc.query("SELECT * FROM sources ORDER BY id DESC", mapper); }
  public Optional<Source> find(long id) { return jdbc.query("SELECT * FROM sources WHERE id=?", mapper, id).stream().findFirst(); }
  public Source create(Source source, String secretEnvRef) {
    jdbc.update("""
        INSERT INTO sources (name,type,base_url,adapter_type,mode,compliance_status,trust_level,scope,enabled,secret_env_ref)
        VALUES (?,?,?,?,?,?,?,?,?,?)""", source.name(), source.type(), source.baseUrl(), source.adapterType(), source.mode(),
        source.complianceStatus(), source.trustLevel(), source.companyCategory(), source.enabled(), secretEnvRef);
    return find(jdbc.queryForObject("SELECT last_insert_rowid()", Long.class)).orElseThrow();
  }
  public Source update(long id, Source source, String secretEnvRef) {
    int changed = jdbc.update("""
        UPDATE sources SET name=?,type=?,base_url=?,adapter_type=?,mode=?,compliance_status=?,
        trust_level=?,scope=?,enabled=?,secret_env_ref=COALESCE(?, secret_env_ref),config_version=config_version+1,
        updated_at=CURRENT_TIMESTAMP WHERE id=?""", source.name(), source.type(), source.baseUrl(), source.adapterType(),
        source.mode(), source.complianceStatus(), source.trustLevel(), source.companyCategory(), source.enabled(), secretEnvRef, id);
    if (changed == 0) throw new MissingResourceException("Source not found");
    return find(id).orElseThrow();
  }
  public void delete(long id) {
    if (jdbc.update("DELETE FROM sources WHERE id=?", id) == 0) throw new MissingResourceException("Source not found");
  }
}

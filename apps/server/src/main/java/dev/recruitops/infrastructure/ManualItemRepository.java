package dev.recruitops.infrastructure;

import dev.recruitops.domain.ManualItem;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ManualItemRepository {
  private final JdbcTemplate jdbc;
  private final JsonValues json;

  public ManualItemRepository(JdbcTemplate jdbc, JsonValues json) { this.jdbc = jdbc; this.json = json; }

  public List<ManualItem> list() {
    return jdbc.query("SELECT * FROM manual_items ORDER BY created_at DESC, id DESC", this::map);
  }

  public ManualItem create(ManualInput input) {
    jdbc.update("""
        INSERT INTO manual_items (platform,company_name,job_title,search_keywords,source_url,
        tech_stack_keywords,certificate_notes,required_action,requires_login,requires_captcha,
        apply_method,reminder_at,priority,status,notes)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", input.platform(), input.companyName(), input.jobTitle(),
        json.write(input.searchKeywords()), input.sourceUrl(), json.write(input.techStackKeywords()),
        input.certificateNotes(), input.requiredAction(), input.requiresLogin(), input.requiresCaptcha(),
        input.applyMethod(), input.reminderAt(), input.priority(), input.status(), input.notes());
    long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    return jdbc.queryForObject("SELECT * FROM manual_items WHERE id=?", this::map, id);
  }

  private ManualItem map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
    return new ManualItem(rs.getLong("id"), rs.getString("platform"), rs.getString("company_name"),
        rs.getString("job_title"), json.strings(rs.getString("search_keywords")), rs.getString("source_url"),
        json.strings(rs.getString("tech_stack_keywords")), rs.getString("certificate_notes"),
        rs.getString("required_action"), rs.getBoolean("requires_login"), rs.getBoolean("requires_captcha"),
        rs.getString("apply_method"), rs.getString("reminder_at"), rs.getInt("priority"),
        rs.getString("status"), rs.getString("notes"), rs.getString("created_at"));
  }

  public record ManualInput(String platform, String companyName, String jobTitle, List<String> searchKeywords,
      String sourceUrl, List<String> techStackKeywords, String certificateNotes, String requiredAction,
      boolean requiresLogin, boolean requiresCaptcha, String applyMethod, String reminderAt, int priority,
      String status, String notes) {}
}

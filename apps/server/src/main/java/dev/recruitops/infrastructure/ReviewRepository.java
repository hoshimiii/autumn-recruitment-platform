package dev.recruitops.infrastructure;

import dev.recruitops.domain.ReviewItem;
import dev.recruitops.domain.ReviewStatus;
import dev.recruitops.domain.ReviewView;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRepository {
  private final JdbcTemplate jdbc; private final JsonValues json; private final RowMapper<ReviewItem> mapper;
  public ReviewRepository(JdbcTemplate jdbc, JsonValues json) {
    this.jdbc = jdbc; this.json = json; this.mapper = (rs, row) -> new ReviewItem(rs.getLong("id"), rs.getLong("source_id"),
      ReviewStatus.valueOf(rs.getString("status")), json.tree(rs.getString("candidate_payload")), json.tree(rs.getString("normalized_payload")),
      rs.getString("review_notes"), nullableLong(rs, "published_job_id"));
  }
  public List<ReviewView> list() {
    return jdbc.query("""
        SELECT r.*, s.name AS source_name FROM review_items r
        LEFT JOIN sources s ON s.id=r.source_id ORDER BY r.id DESC
        """, (rs, row) -> view(rs));
  }
  public Optional<ReviewItem> find(long id) { return jdbc.query("SELECT * FROM review_items WHERE id=?", mapper, id).stream().findFirst(); }
  public void publish(long id, long jobId, String payload) { jdbc.update("UPDATE review_items SET status='PUBLISHED',published_job_id=?,normalized_payload=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", jobId, payload, id); }
  public void transition(long id, ReviewStatus status, String notes) { jdbc.update("UPDATE review_items SET status=?,review_notes=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", status.name(), notes, id); }
  public void audit(long id, String action, String before, String after, String notes) { jdbc.update("INSERT INTO audit_logs (entity_type,entity_id,action,before_payload,after_payload,notes) VALUES ('REVIEW_ITEM',?,?,?,?,?)", id, action, before, after, notes); }
  public void snapshot(long jobId, long sourceId, String sourceUrl, String payload) { jdbc.update("INSERT INTO job_snapshots (job_id,source_id,source_url,parsed_payload) VALUES (?,?,?,?)", jobId, sourceId, sourceUrl, payload); }
  private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    long value = rs.getLong(column); return rs.wasNull() ? null : value;
  }
  private ReviewView view(java.sql.ResultSet rs) throws java.sql.SQLException {
    var payload = json.tree(rs.getString("normalized_payload"));
    return new ReviewView(rs.getLong("id"), nullableLong(rs, "source_id"), rs.getString("source_name"),
        text(payload, "companyName"), text(payload, "title"), rs.getString("review_notes"),
        nullableDouble(rs, "confidence"), ReviewStatus.valueOf(rs.getString("status")),
        rs.getString("created_at"), payload);
  }
  private static String text(com.fasterxml.jackson.databind.JsonNode payload, String field) {
    return payload.hasNonNull(field) ? payload.get(field).asText() : null;
  }
  private static Double nullableDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    double value = rs.getDouble(column); return rs.wasNull() ? null : value;
  }
}

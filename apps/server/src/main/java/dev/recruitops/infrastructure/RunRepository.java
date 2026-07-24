package dev.recruitops.infrastructure;

import dev.recruitops.domain.CollectionRun;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RunRepository {
  private final JdbcTemplate jdbc;
  public RunRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public List<CollectionRun> list() {
    return jdbc.query("SELECT * FROM collection_runs ORDER BY id DESC", this::map);
  }
  public Optional<CollectionRun> find(long id) {
    return jdbc.query("SELECT * FROM collection_runs WHERE id=?", this::map, id).stream().findFirst();
  }
  public CollectionRun start(Long sourceId, String triggerType) {
    jdbc.update("""
        INSERT INTO collection_runs (source_id,trigger_type,status,started_at)
        VALUES (?,?,'RUNNING',CURRENT_TIMESTAMP)
        """, sourceId, triggerType);
    return find(jdbc.queryForObject("SELECT last_insert_rowid()", Long.class)).orElseThrow();
  }
  public CollectionRun finish(long id, String status, int discovered, int reviewItems,
                              int errors, String summary) {
    jdbc.update("""
        UPDATE collection_runs SET status=?,finished_at=CURRENT_TIMESTAMP,discovered_count=?,
        review_item_count=?,error_count=?,error_summary=? WHERE id=?
        """, status, discovered, reviewItems, errors, summary, id);
    return find(id).orElseThrow();
  }
  private CollectionRun map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
    return new CollectionRun(rs.getLong("id"), nullableLong(rs, "source_id"), rs.getString("trigger_type"),
        rs.getString("status"), rs.getString("started_at"), rs.getString("finished_at"),
        rs.getInt("discovered_count"), rs.getInt("review_item_count"), rs.getInt("error_count"),
        rs.getString("error_summary"));
  }
  private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    long value = rs.getLong(column); return rs.wasNull() ? null : value;
  }
}

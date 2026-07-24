package dev.recruitops.infrastructure;

import dev.recruitops.domain.CollectionRun;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RunRepository {
  private final JdbcTemplate jdbc;
  public RunRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public List<CollectionRun> list() { return jdbc.query("SELECT * FROM collection_runs ORDER BY id DESC", (rs, row) ->
      new CollectionRun(rs.getLong("id"), nullableLong(rs, "source_id"), rs.getString("trigger_type"),
          rs.getString("status"), rs.getString("started_at"), rs.getString("finished_at"), rs.getInt("discovered_count"),
          rs.getInt("review_item_count"), rs.getInt("error_count"), rs.getString("error_summary"))); }
  private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    long value = rs.getLong(column); return rs.wasNull() ? null : value;
  }
}

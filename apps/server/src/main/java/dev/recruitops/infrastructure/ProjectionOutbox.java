package dev.recruitops.infrastructure;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectionOutbox {
  private final JdbcTemplate jdbc;
  public ProjectionOutbox(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public long version() { return jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM job_projection_outbox", Long.class); }
  public boolean pending() { return jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM job_projection_outbox WHERE processed_at IS NULL)", Integer.class) == 1; }
  public List<Event> next() { return jdbc.query("SELECT id,job_id FROM job_projection_outbox WHERE processed_at IS NULL ORDER BY id LIMIT 100", (rs,row)->new Event(rs.getLong(1),rs.getLong(2))); }
  public void done(long id) { jdbc.update("UPDATE job_projection_outbox SET processed_at=CURRENT_TIMESTAMP WHERE id=?", id); }
  public int rebuild() { return jdbc.update("INSERT INTO job_projection_outbox(job_id) SELECT id FROM jobs"); }
  public record Event(long id, long jobId) {}
}

package dev.recruitops.infrastructure;

import dev.recruitops.domain.Job;
import dev.recruitops.domain.JobSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JobRepository {
  private final JdbcTemplate jdbc; private final JsonValues json; private final RowMapper<Job> mapper;
  public JobRepository(JdbcTemplate jdbc, JsonValues json) {
    this.jdbc = jdbc; this.json = json; this.mapper = (rs, row) -> new Job(rs.getLong("id"), rs.getString("company_type"),
      rs.getString("company_name"), rs.getString("title"), rs.getString("job_family"), rs.getString("job_type"),
      rs.getString("city"), rs.getString("deadline"), rs.getString("status"), rs.getInt("priority"),
      json.strings(rs.getString("tech_stack")), json.strings(rs.getString("certificate_requirements")),
      rs.getString("source_url"), rs.getString("apply_url"), rs.getString("apply_method"),
      rs.getBoolean("requires_login"), rs.getBoolean("requires_captcha"), rs.getString("source_trust_level"),
      rs.getString("source_name"), rs.getString("source_type"), rs.getString("captured_at"),
      rs.getString("updated_at"), rs.getString("review_status"), nullableDouble(rs, "confidence"),
      rs.getString("jd"), rs.getString("requirements"));
  }
  public Page list(JobFilter filter) {
    List<Object> args = new ArrayList<>(); StringBuilder where = new StringBuilder(" WHERE j.status='PUBLISHED' AND (j.deadline IS NULL OR j.deadline='' OR j.deadline >= date('now'))");
    add(where, args, "(j.company_name LIKE ? OR j.title LIKE ? OR j.jd LIKE ?)", filter.keyword(), 3);
    add(where, args, "j.company_type=?", filter.companyCategory(), 1); add(where, args, "j.company_name=?", filter.company(), 1);
    add(where, args, "j.job_type=?", filter.jobType(), 1); add(where, args, "j.city=?", filter.city(), 1);
    add(where, args, "j.tech_stack LIKE ?", filter.techStack(), 1); add(where, args, "j.certificate_requirements LIKE ?", filter.certificate(), 1);
    add(where, args, "j.deadline >= ?", filter.deadlineFrom(), 1); add(where, args, "j.deadline <= ?", filter.deadlineTo(), 1);
    add(where, args, "j.source_trust_level=?", filter.sourceTrust(), 1); add(where, args, "j.updated_at >= ?", filter.changedSince(), 1);
    int total = jdbc.queryForObject("SELECT COUNT(*) FROM jobs j" + where, Integer.class, args.toArray());
    int page = Math.max(1, filter.page()); int size = Math.min(100, Math.max(1, filter.pageSize()));
    List<Object> paged = new ArrayList<>(args); paged.add(size); paged.add((page - 1) * size);
    return new Page(jdbc.query(select() + where + " ORDER BY j.updated_at DESC, j.id DESC LIMIT ? OFFSET ?", mapper, paged.toArray()), page, size, total);
  }
  public Optional<Job> find(long id) { return jdbc.query(select() + " WHERE j.id=? AND j.status='PUBLISHED'", mapper, id).stream().findFirst(); }
  public List<JobSnapshot> snapshots(long jobId) {
    return jdbc.query("""
        SELECT id,source_id,source_url,content_hash,parsed_payload,captured_at
        FROM job_snapshots WHERE job_id=? ORDER BY captured_at DESC,id DESC
        """, (rs, row) -> new JobSnapshot(rs.getLong("id"), nullableLong(rs, "source_id"),
          rs.getString("source_url"), rs.getString("content_hash"),
          json.tree(rs.getString("parsed_payload") == null ? "{}" : rs.getString("parsed_payload")),
          rs.getString("captured_at")), jobId);
  }
  public SavedJob upsert(JobDraft draft, long sourceId) {
    Optional<Long> existing = jdbc.queryForList(
        "SELECT id FROM jobs WHERE dedupe_key=? ORDER BY id LIMIT 1", Long.class, draft.dedupeKey())
        .stream().findFirst();
    if (existing.isPresent()) {
      jdbc.update("""
          UPDATE jobs SET company_type=?,company_name=?,title=?,job_family=?,job_type=?,city=?,
          deadline=?,status='PUBLISHED',priority=?,tech_stack=?,certificate_requirements=?,source_id=?,
          source_url=?,apply_url=?,apply_method=?,requires_login=?,requires_captcha=?,source_trust_level=?,
          jd=?,requirements=?,updated_at=CURRENT_TIMESTAMP WHERE id=?
          """, draft.companyCategory(), draft.companyName(), draft.title(), draft.jobFamily(),
          draft.jobType(), draft.city(), draft.deadline(), draft.priority(), json.write(draft.techStack()),
          json.write(draft.certificateRequirements()), sourceId, draft.sourceUrl(), draft.applyUrl(),
          draft.applyMethod(), draft.requiresLogin(), draft.requiresCaptcha(), draft.sourceTrust(),
          draft.jd(), draft.requirements(), existing.get());
      return new SavedJob(findById(existing.get()), false);
    }
    jdbc.update("""
        INSERT INTO jobs (company_type,company_name,title,job_family,job_type,city,deadline,status,
        priority,tech_stack,certificate_requirements,source_id,source_url,apply_url,apply_method,
        requires_login,requires_captcha,source_trust_level,jd,requirements,dedupe_key)
        VALUES (?,?,?,?,?,?,?,'PUBLISHED',?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, draft.companyCategory(), draft.companyName(), draft.title(), draft.jobFamily(),
        draft.jobType(), draft.city(), draft.deadline(), draft.priority(), json.write(draft.techStack()),
        json.write(draft.certificateRequirements()), sourceId, draft.sourceUrl(), draft.applyUrl(),
        draft.applyMethod(), draft.requiresLogin(), draft.requiresCaptcha(), draft.sourceTrust(),
        draft.jd(), draft.requirements(), draft.dedupeKey());
    long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    return new SavedJob(findById(id), true);
  }
  private Job findById(long id) { return jdbc.query(select() + " WHERE j.id=?", mapper, id).stream().findFirst().orElseThrow(); }
  private String select() {
    return """
        SELECT j.*, s.name AS source_name, s.type AS source_type,
        COALESCE(snapshot.captured_at, j.created_at) AS captured_at, review.status AS review_status,
        review.confidence AS confidence FROM jobs j
        LEFT JOIN sources s ON s.id=j.source_id
        LEFT JOIN job_snapshots snapshot ON snapshot.id=(SELECT id FROM job_snapshots WHERE job_id=j.id ORDER BY captured_at DESC, id DESC LIMIT 1)
        LEFT JOIN review_items review ON review.id=(SELECT id FROM review_items WHERE published_job_id=j.id ORDER BY updated_at DESC, id DESC LIMIT 1)
        """;
  }
  private void add(StringBuilder where, List<Object> args, String sql, String value, int copies) {
    if (value == null || value.isBlank()) return; where.append(" AND ").append(sql);
    for (int i = 0; i < copies; i++) args.add(copies == 3 ? "%" + value + "%" : (sql.contains("LIKE") ? "%" + value + "%" : value));
  }
  private static Double nullableDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    double value = rs.getDouble(column); return rs.wasNull() ? null : value;
  }
  private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    long value = rs.getLong(column); return rs.wasNull() ? null : value;
  }
  public record JobFilter(String keyword, String companyCategory, String company, String jobType, String city,
      String techStack, String certificate, String deadlineFrom, String deadlineTo, String sourceTrust,
      String changedSince, int page, int pageSize) {}
  public record Page(List<Job> items, int page, int pageSize, int total) {}
  public record JobDraft(
      String companyCategory, String companyName, String title, String jobFamily, String jobType,
      String city, String deadline, int priority, List<String> techStack,
      List<String> certificateRequirements, String sourceUrl, String applyUrl, String applyMethod,
      boolean requiresLogin, boolean requiresCaptcha, String sourceTrust, String jd,
      String requirements, String dedupeKey) {}
  public record SavedJob(Job job, boolean created) {}
}

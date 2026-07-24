package dev.recruitops.infrastructure;

import dev.recruitops.domain.Job;
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
      rs.getString("company_name"), rs.getString("title"), rs.getString("job_type"), rs.getString("city"),
      rs.getString("deadline"), rs.getString("status"), json.strings(rs.getString("tech_stack")),
      json.strings(rs.getString("certificate_requirements")), rs.getString("source_url"), rs.getString("apply_url"),
      rs.getString("source_trust_level"), rs.getString("jd"), rs.getString("requirements"));
  }
  public Page list(JobFilter filter) {
    List<Object> args = new ArrayList<>(); StringBuilder where = new StringBuilder(" WHERE status='PUBLISHED' AND (deadline IS NULL OR deadline='' OR deadline >= date('now'))");
    add(where, args, "(company_name LIKE ? OR title LIKE ? OR jd LIKE ?)", filter.keyword(), 3);
    add(where, args, "company_type=?", filter.companyCategory(), 1); add(where, args, "company_name=?", filter.company(), 1);
    add(where, args, "job_type=?", filter.jobType(), 1); add(where, args, "city=?", filter.city(), 1);
    add(where, args, "tech_stack LIKE ?", filter.techStack(), 1); add(where, args, "certificate_requirements LIKE ?", filter.certificate(), 1);
    add(where, args, "deadline >= ?", filter.deadlineFrom(), 1); add(where, args, "deadline <= ?", filter.deadlineTo(), 1);
    add(where, args, "source_trust_level=?", filter.sourceTrust(), 1); add(where, args, "updated_at >= ?", filter.changedSince(), 1);
    int total = jdbc.queryForObject("SELECT COUNT(*) FROM jobs" + where, Integer.class, args.toArray());
    int page = Math.max(1, filter.page()); int size = Math.min(100, Math.max(1, filter.pageSize()));
    List<Object> paged = new ArrayList<>(args); paged.add(size); paged.add((page - 1) * size);
    return new Page(jdbc.query("SELECT * FROM jobs" + where + " ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?", mapper, paged.toArray()), page, size, total);
  }
  public Optional<Job> find(long id) { return jdbc.query("SELECT * FROM jobs WHERE id=? AND status='PUBLISHED'", mapper, id).stream().findFirst(); }
  public Job insert(JobDraft draft, long sourceId) {
    jdbc.update("""
        INSERT INTO jobs (company_type,company_name,title,job_type,city,deadline,status,tech_stack,
        certificate_requirements,source_id,source_url,apply_url,source_trust_level,jd,requirements,dedupe_key)
        VALUES (?,?,?,?,?,?, 'PUBLISHED',?,?,?,?,?,?,?,?,?)""", draft.companyType(), draft.companyName(), draft.title(),
        draft.jobType(), draft.city(), draft.deadline(), json.write(draft.techStack()), json.write(draft.certificateRequirements()),
        sourceId, draft.sourceUrl(), draft.applyUrl(), draft.sourceTrustLevel(), draft.jd(), draft.requirements(), draft.dedupeKey());
    return findById(jdbc.queryForObject("SELECT last_insert_rowid()", Long.class));
  }
  private Job findById(long id) { return jdbc.query("SELECT * FROM jobs WHERE id=?", mapper, id).stream().findFirst().orElseThrow(); }
  private void add(StringBuilder where, List<Object> args, String sql, String value, int copies) {
    if (value == null || value.isBlank()) return; where.append(" AND ").append(sql);
    for (int i = 0; i < copies; i++) args.add(copies == 3 ? "%" + value + "%" : (sql.contains("LIKE") ? "%" + value + "%" : value));
  }
  public record JobFilter(String keyword, String companyCategory, String company, String jobType, String city,
      String techStack, String certificate, String deadlineFrom, String deadlineTo, String sourceTrust,
      String changedSince, int page, int pageSize) {}
  public record Page(List<Job> items, int page, int pageSize, int total) {}
  public record JobDraft(String companyType, String companyName, String title, String jobType, String city,
      String deadline, List<String> techStack, List<String> certificateRequirements, String sourceUrl,
      String applyUrl, String sourceTrustLevel, String jd, String requirements, String dedupeKey) {}
}

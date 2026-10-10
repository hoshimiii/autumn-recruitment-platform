package dev.recruitops.infrastructure;

import dev.recruitops.domain.DiscoveredCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CandidateRepository {
  private final JdbcTemplate jdbc;
  private final JsonValues json;

  public CandidateRepository(JdbcTemplate jdbc, JsonValues json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public boolean exists(long sourceId, String dedupeKey, String contentHash) {
    return jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM review_items
        WHERE source_id=? AND dedupe_key=? AND content_hash=?)
        """, Integer.class, sourceId, dedupeKey, contentHash) == 1;
  }

  public int insertIfAbsent(long sourceId, long runId, DiscoveredCandidate candidate) {
    return jdbc.update("""
        INSERT INTO review_items (source_id,run_id,candidate_payload,normalized_payload,dedupe_key,
        content_hash,confidence,status) VALUES (?,?,?,?,?,?,?,'PENDING_REVIEW')
        ON CONFLICT(source_id,dedupe_key,content_hash) WHERE content_hash IS NOT NULL DO NOTHING
        """, sourceId, runId, json.write(candidate.candidatePayload()),
        json.write(candidate.normalizedPayload()), candidate.dedupeKey(), candidate.contentHash(),
        candidate.confidence());
  }
}

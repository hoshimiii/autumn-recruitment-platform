package dev.recruitops.application;

import dev.recruitops.domain.DiscoveredCandidate;
import dev.recruitops.infrastructure.CandidateRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidatePersistence {
  private final CandidateRepository candidates;
  public CandidatePersistence(CandidateRepository candidates) { this.candidates = candidates; }

  @Transactional
  public int store(long sourceId, long runId, List<DiscoveredCandidate> discovered) {
    int inserted = 0;
    for (DiscoveredCandidate candidate : discovered) {
      inserted += candidates.insertIfAbsent(sourceId, runId, candidate);
    }
    return inserted;
  }
}

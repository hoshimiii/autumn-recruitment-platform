package dev.recruitops.application;

import dev.recruitops.infrastructure.ElasticJobIndex;
import dev.recruitops.infrastructure.JobRepository;
import dev.recruitops.infrastructure.ProjectionOutbox;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class JobProjectionService {
  private final ElasticJobIndex index; private final JobRepository jobs; private final ProjectionOutbox outbox;
  private volatile String lastError="";
  public JobProjectionService(ElasticJobIndex index,JobRepository jobs,ProjectionOutbox outbox) {
    this.index=index;this.jobs=jobs;this.outbox=outbox;
  }
  @PreAuthorize("@operationAuthorizer.can('ADMIN')")
  public int rebuild() { return outbox.rebuild(); }
  @PreAuthorize("@operationAuthorizer.can('ADMIN')")
  public Map<String,Object> status() { return Map.of("enabled",index.enabled(),"pending",outbox.pending(),"lastError",lastError); }
  @Scheduled(initialDelayString="${recruitops.elasticsearch.sync-delay-ms:10000}",fixedDelayString="${recruitops.elasticsearch.sync-delay-ms:10000}")
  public synchronized void synchronize() {
    if(!index.enabled()) return;
    try {
      var events=outbox.next();
      for(var event:events) {
        var job=jobs.find(event.jobId());
        if(job.isPresent()) index.upsert(job.get(),event.id()); else index.delete(event.jobId(),event.id());
      }
      if(!events.isEmpty()) {
        index.refresh();
        for(var event:events) outbox.done(event.id());
      }
      lastError="";
    } catch(RuntimeException unavailable) { lastError="Elasticsearch projection unavailable; SQL fallback is active"; }
  }
}

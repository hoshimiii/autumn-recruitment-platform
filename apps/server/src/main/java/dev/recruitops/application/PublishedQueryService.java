package dev.recruitops.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.infrastructure.ElasticJobIndex;
import dev.recruitops.infrastructure.JobRepository;
import dev.recruitops.infrastructure.ProjectionOutbox;
import dev.recruitops.infrastructure.RedisJobCache;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class PublishedQueryService {
  private final JobRepository jobs; private final ElasticJobIndex index; private final RedisJobCache cache;
  private final ProjectionOutbox outbox; private final ObjectMapper json;
  private final dev.recruitops.infrastructure.DatasetNamespace namespace;
  public PublishedQueryService(JobRepository jobs,ElasticJobIndex index,RedisJobCache cache,ProjectionOutbox outbox,ObjectMapper json,dev.recruitops.infrastructure.DatasetNamespace namespace) {
    this.jobs=jobs;this.index=index;this.cache=cache;this.outbox=outbox;this.json=json;
    this.namespace=namespace;
  }
  public JobRepository.Page list(JobRepository.JobFilter filter) {
    long version=outbox.version();
    // Cache keys contain the SQL outbox generation: committed edits never reuse the old cache generation.
    boolean searchable=index.enabled() && !outbox.pending();
    String key="recruitops:jobs:"+namespace.value()+":"+version+":"+LocalDate.now(ZoneOffset.UTC)+":"+searchable+":"+hash(filter.toString());
    var cached=cache.get(key);
    if(cached.isPresent()) {
      try { var page=json.readValue(cached.get(),JobRepository.Page.class); if(outbox.version()==version) return page; }
      catch(java.io.IOException invalidCache) { /* Invalid cached bytes are a cache miss. */ }
    }
    var ids=searchable?index.searchIds(filter.keyword()):java.util.Optional.<java.util.List<Long>>empty();
    JobRepository.Page result=ids.isPresent()?jobs.list(filter,ids.get()):jobs.list(filter);
    // Do not cache outage fallback under an ES-ready key; recovery must retry fuzzy search.
    if(outbox.version()==version && (!searchable || filter.keyword()==null || filter.keyword().isBlank() || ids.isPresent())) {
      try { cache.put(key,json.writeValueAsString(result)); } catch(java.io.IOException ignored) { }
    }
    return result;
  }
  private String hash(String text) {
    try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
    catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
  }
}

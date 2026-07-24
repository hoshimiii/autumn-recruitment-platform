package dev.recruitops.application;

import dev.recruitops.domain.Job;
import dev.recruitops.infrastructure.JobRepository;
import dev.recruitops.infrastructure.JsonValues;
import dev.recruitops.infrastructure.MissingResourceException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JobService {
  private final JobRepository jobs;
  private final JsonValues json;
  public JobService(JobRepository jobs, JsonValues json) { this.jobs = jobs; this.json = json; }
  public JobRepository.Page list(JobRepository.JobFilter filter) { return jobs.list(filter); }
  public Map<String, Object> detail(long id) {
    Job job = jobs.find(id).orElseThrow(() -> new MissingResourceException("Job not found"));
    Map<String, Object> detail = new LinkedHashMap<>(json.map(job));
    detail.put("snapshots", jobs.snapshots(id));
    return detail;
  }
}

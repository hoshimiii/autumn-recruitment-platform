package dev.recruitops.application;

import dev.recruitops.domain.Job;
import dev.recruitops.infrastructure.JobRepository;
import dev.recruitops.infrastructure.MissingResourceException;
import org.springframework.stereotype.Service;

@Service
public class JobService {
  private final JobRepository jobs;
  public JobService(JobRepository jobs) { this.jobs = jobs; }
  public JobRepository.Page list(JobRepository.JobFilter filter) { return jobs.list(filter); }
  public Job detail(long id) { return jobs.find(id).orElseThrow(() -> new MissingResourceException("Job not found")); }
}

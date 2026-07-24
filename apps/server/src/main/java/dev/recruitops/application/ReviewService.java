package dev.recruitops.application;

import com.fasterxml.jackson.databind.JsonNode;
import dev.recruitops.domain.Job;
import dev.recruitops.domain.ReviewItem;
import dev.recruitops.domain.ReviewStatus;
import dev.recruitops.infrastructure.JobRepository;
import dev.recruitops.infrastructure.JsonValues;
import dev.recruitops.infrastructure.MissingResourceException;
import dev.recruitops.infrastructure.ReviewRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
  private final ReviewRepository reviews; private final JobRepository jobs; private final JsonValues json;
  public ReviewService(ReviewRepository reviews, JobRepository jobs, JsonValues json) { this.reviews = reviews; this.jobs = jobs; this.json = json; }
  public List<ReviewItem> list() { return reviews.list(); }
  @Transactional
  public Job approve(long id, JsonNode edited) {
    ReviewItem item = item(id);
    if (item.status() == ReviewStatus.PUBLISHED && item.publishedJobId() != null) return jobs.find(item.publishedJobId()).orElseThrow();
    if (item.status() != ReviewStatus.PENDING_REVIEW) throw new IllegalStateException("Review item is not pending");
    JsonNode payload = edited == null || edited.isNull() ? item.normalizedPayload() : edited;
    JobRepository.JobDraft draft = draft(payload, item.id());
    Job job = jobs.insert(draft, item.sourceId()); String after = json.write(payload);
    reviews.snapshot(job.id(), item.sourceId(), draft.sourceUrl(), after);
    reviews.publish(id, job.id(), after); reviews.audit(id, "APPROVED", json.write(item.normalizedPayload()), after, null);
    return job;
  }
  @Transactional
  public ReviewItem reject(long id, String reason) { return transition(id, ReviewStatus.REJECTED, reason, "REJECTED"); }
  @Transactional
  public ReviewItem block(long id, String reason) { return transition(id, ReviewStatus.BLOCKED, reason, "BLOCKED"); }
  private ReviewItem transition(long id, ReviewStatus status, String reason, String action) {
    ReviewItem item = item(id); if (item.status() != ReviewStatus.PENDING_REVIEW) throw new IllegalStateException("Review item is not pending");
    reviews.transition(id, status, reason); reviews.audit(id, action, item.status().name(), status.name(), reason); return item(id);
  }
  private ReviewItem item(long id) { return reviews.find(id).orElseThrow(() -> new MissingResourceException("Review item not found")); }
  private JobRepository.JobDraft draft(JsonNode node, long id) {
    return new JobRepository.JobDraft(required(node, "companyType"), required(node, "companyName"), required(node, "title"),
        required(node, "jobType"), text(node, "city"), text(node, "deadline"), strings(node, "techStack"),
        strings(node, "certificateRequirements"), required(node, "sourceUrl"), text(node, "applyUrl"),
        required(node, "sourceTrustLevel"), text(node, "jd"), text(node, "requirements"), "review:" + id);
  }
  private String required(JsonNode node, String key) { String value = text(node, key); if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing required field: " + key); return value; }
  private String text(JsonNode node, String key) { return node.hasNonNull(key) ? node.get(key).asText() : null; }
  private List<String> strings(JsonNode node, String key) { return node.has(key) && node.get(key).isArray() ? java.util.stream.StreamSupport.stream(node.get(key).spliterator(), false).map(JsonNode::asText).toList() : List.of(); }
}

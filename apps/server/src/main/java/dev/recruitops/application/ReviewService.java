package dev.recruitops.application;

import com.fasterxml.jackson.databind.JsonNode;
import dev.recruitops.domain.Job;
import dev.recruitops.domain.ReviewItem;
import dev.recruitops.domain.ReviewDetail;
import dev.recruitops.domain.ReviewStatus;
import dev.recruitops.domain.ReviewView;
import dev.recruitops.infrastructure.JobRepository;
import dev.recruitops.infrastructure.JsonValues;
import dev.recruitops.infrastructure.MissingResourceException;
import dev.recruitops.infrastructure.ReviewRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
  private final ReviewRepository reviews; private final JobRepository jobs; private final JsonValues json;
  public ReviewService(ReviewRepository reviews, JobRepository jobs, JsonValues json) { this.reviews = reviews; this.jobs = jobs; this.json = json; }
  @PreAuthorize("@operationAuthorizer.can('REVIEWER')")
  public List<ReviewView> list() { return reviews.list(); }
  @PreAuthorize("@operationAuthorizer.can('REVIEWER')")
  public ReviewDetail detail(long id) {
    return reviews.detail(id)
        .orElseThrow(() -> new MissingResourceException("Review item not found"));
  }
  @Transactional
  @PreAuthorize("@operationAuthorizer.can('REVIEWER')")
  public Job approve(long id, JsonNode edited) {
    boolean claimed = reviews.claimPending(id);
    ReviewItem item = item(id);
    if (item.status() == ReviewStatus.PUBLISHED && item.publishedJobId() != null) return jobs.find(item.publishedJobId()).orElseThrow();
    if (!claimed || item.status() != ReviewStatus.PENDING_REVIEW) throw new IllegalStateException("Review item is not pending");
    JsonNode payload = edited == null || edited.isNull() ? item.normalizedPayload() : edited;
    JobRepository.JobDraft draft = draft(payload, item);
    JobRepository.SavedJob saved = jobs.upsert(draft, item.sourceId());
    Job job = saved.job(); String after = json.write(payload);
    reviews.snapshot(job.id(), item.sourceId(), draft.sourceUrl(), item.contentHash(),
        json.write(item.candidatePayload()), after);
    reviews.publish(id, job.id(), after);
    reviews.audit(id, saved.created() ? "APPROVED" : "UPDATED",
        json.write(item.normalizedPayload()), after, null);
    return jobs.find(job.id()).orElseThrow();
  }
  @Transactional
  @PreAuthorize("@operationAuthorizer.can('REVIEWER')")
  public ReviewItem reject(long id, String reason) { return transition(id, ReviewStatus.REJECTED, reason, "REJECTED"); }
  @Transactional
  @PreAuthorize("@operationAuthorizer.can('REVIEWER')")
  public ReviewItem block(long id, String reason) { return transition(id, ReviewStatus.BLOCKED, reason, "BLOCKED"); }
  private ReviewItem transition(long id, ReviewStatus status, String reason, String action) {
    boolean claimed = reviews.claimPending(id);
    ReviewItem item = item(id); if (!claimed || item.status() != ReviewStatus.PENDING_REVIEW) throw new IllegalStateException("Review item is not pending");
    reviews.transition(id, status, reason); reviews.audit(id, action, item.status().name(), status.name(), reason); return item(id);
  }
  private ReviewItem item(long id) { return reviews.find(id).orElseThrow(() -> new MissingResourceException("Review item not found")); }
  private JobRepository.JobDraft draft(JsonNode node, ReviewItem item) {
    return new JobRepository.JobDraft(required(node, "companyCategory", "companyType"),
        required(node, "companyName"), required(node, "title"), text(node, "jobFamily"),
        required(node, "jobType"), text(node, "city"), text(node, "deadline"),
        integer(node, "priority"), strings(node, "techStack"), strings(node, "certificateRequirements"),
        required(node, "sourceUrl"), text(node, "applyUrl"), text(node, "applyMethod"),
        bool(node, "requiresLogin"), bool(node, "requiresCaptcha"),
        required(node, "sourceTrust", "sourceTrustLevel"), text(node, "jd"),
        text(node, "requirements"), CandidateNormalizer.identity(item.sourceId(), node),
        text(node, "businessUnit"), cohort(node), text(node, "sourceRowId"), text(node, "officialJobId"));
  }
  private Integer cohort(JsonNode node) {
    String value = text(node, "recruitmentYear");
    if (value == null || value.isBlank()) return null;
    if (!value.matches("20\\d{2}")) throw new IllegalArgumentException("Invalid recruitmentYear");
    return Integer.valueOf(value);
  }
  private String required(JsonNode node, String... keys) {
    for (String key : keys) { String value = text(node, key); if (value != null && !value.isBlank()) return value; }
    throw new IllegalArgumentException("Missing required field: " + keys[0]);
  }
  private String text(JsonNode node, String key) { return node.hasNonNull(key) ? node.get(key).asText() : null; }
  private List<String> strings(JsonNode node, String key) { return node.has(key) && node.get(key).isArray() ? java.util.stream.StreamSupport.stream(node.get(key).spliterator(), false).map(JsonNode::asText).toList() : List.of(); }
  private int integer(JsonNode node, String key) { return node.path(key).asInt(0); }
  private boolean bool(JsonNode node, String key) { return node.path(key).asBoolean(false); }
}

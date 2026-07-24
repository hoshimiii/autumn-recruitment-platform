package dev.recruitops.web;

import com.fasterxml.jackson.databind.JsonNode;
import dev.recruitops.application.ReviewService;
import dev.recruitops.domain.Job;
import dev.recruitops.domain.ReviewItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/review-items")
public class ReviewController {
  private final ReviewService reviews;
  public ReviewController(ReviewService reviews) { this.reviews = reviews; }
  @GetMapping public java.util.Map<String, List<ReviewItem>> list() { return java.util.Map.of("items", reviews.list()); }
  @PostMapping("/{id}/approve") public Job approve(@PathVariable long id, @RequestBody(required=false) ApproveRequest request) {
    return reviews.approve(id, request == null ? null : request.normalizedPayload());
  }
  @PostMapping("/{id}/reject") public ReviewItem reject(@PathVariable long id, @Valid @RequestBody ReasonRequest request) { return reviews.reject(id, request.reason()); }
  @PostMapping("/{id}/block") public ReviewItem block(@PathVariable long id, @Valid @RequestBody ReasonRequest request) { return reviews.block(id, request.reason()); }
  public record ApproveRequest(JsonNode normalizedPayload) {}
  public record ReasonRequest(@NotBlank String reason) {}
}

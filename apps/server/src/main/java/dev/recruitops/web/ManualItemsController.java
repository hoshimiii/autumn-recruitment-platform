package dev.recruitops.web;

import dev.recruitops.application.ManualItemService;
import dev.recruitops.domain.ManualItem;
import dev.recruitops.infrastructure.ManualItemRepository.ManualInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manual-items")
public class ManualItemsController {
  private final ManualItemService items;

  public ManualItemsController(ManualItemService items) { this.items = items; }

  @GetMapping public java.util.Map<String, List<ManualItem>> list() { return java.util.Map.of("items", items.list()); }

  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public ManualItem create(@Valid @RequestBody ManualRequest request) { return items.create(request.toInput()); }

  public record ManualRequest(@NotBlank String platform, @NotBlank String companyName, String jobTitle,
      List<String> searchKeywords, String sourceUrl, List<String> techStackKeywords, String certificateNotes,
      @NotBlank String requiredAction, Boolean requiresLogin, Boolean requiresCaptcha, String applyMethod,
      String reminderAt, Integer priority, String status, String notes) {
    ManualInput toInput() {
      return new ManualInput(platform, companyName, jobTitle, list(searchKeywords), sourceUrl,
          list(techStackKeywords), certificateNotes, requiredAction, Boolean.TRUE.equals(requiresLogin),
          Boolean.TRUE.equals(requiresCaptcha), applyMethod, reminderAt, priority == null ? 0 : priority,
          status == null || status.isBlank() ? "PENDING" : status, notes);
    }
    private static List<String> list(List<String> value) { return value == null ? List.of() : List.copyOf(value); }
  }
}

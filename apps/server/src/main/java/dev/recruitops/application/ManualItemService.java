package dev.recruitops.application;

import dev.recruitops.domain.ManualItem;
import dev.recruitops.infrastructure.ManualItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ManualItemService {
  private final ManualItemRepository items;

  public ManualItemService(ManualItemRepository items) { this.items = items; }
  public List<ManualItem> list() { return items.list(); }
  public ManualItem create(ManualItemRepository.ManualInput input) { return items.create(input); }
}

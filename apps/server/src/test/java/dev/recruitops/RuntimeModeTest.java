package dev.recruitops;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.recruitops.infrastructure.CollectionScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class RuntimeModeTest {
  @Test
  void nonWebCliAndMcpContextDoesNotStartTheDailyScheduler() {
    try (var context = new SpringApplicationBuilder(RecruitOpsApplication.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:sqlite:file:runtime-mode?mode=memory&cache=shared",
            "spring.main.banner-mode=off",
            "logging.level.root=OFF")
        .run()) {
      assertTrue(context.getBeansOfType(CollectionScheduler.class).isEmpty());
    }
  }
}

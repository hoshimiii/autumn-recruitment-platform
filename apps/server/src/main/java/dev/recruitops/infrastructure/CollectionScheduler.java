package dev.recruitops.infrastructure;

import dev.recruitops.application.CollectionWorkflow;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name="recruitops.collector.scheduler-enabled",havingValue="true",matchIfMissing=true)
public class CollectionScheduler {
  private final CollectionWorkflow workflow;
  public CollectionScheduler(CollectionWorkflow workflow) { this.workflow = workflow; }

  @Scheduled(
      cron = "${recruitops.collector.daily-cron:0 0 8 * * *}",
      zone = "${recruitops.collector.daily-timezone:Asia/Shanghai}")
  public void collectDaily() {
    try {
      workflow.runDaily();
    } catch (IllegalStateException ignored) {
      // The active in-process run remains authoritative.
    }
  }
}

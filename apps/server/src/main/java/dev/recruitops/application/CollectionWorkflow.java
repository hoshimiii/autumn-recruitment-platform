package dev.recruitops.application;

import dev.recruitops.domain.CollectionRun;
import dev.recruitops.domain.Source;
import dev.recruitops.infrastructure.MissingResourceException;
import dev.recruitops.infrastructure.RunRepository;
import dev.recruitops.infrastructure.SourceRepository;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class CollectionWorkflow {
  private final SourceRepository sources;
  private final RunRepository runs;
  private final SourcePolicy policy;
  private final CollectionEngine engine;
  private final CandidatePersistence persistence;
  // ponytail: Replace this with a database lease only when multi-instance deployment is introduced.
  private final AtomicBoolean dailyRunning = new AtomicBoolean();

  public CollectionWorkflow(SourceRepository sources, RunRepository runs, SourcePolicy policy,
                            CollectionEngine engine, CandidatePersistence persistence) {
    this.sources = sources;
    this.runs = runs;
    this.policy = policy;
    this.engine = engine;
    this.persistence = persistence;
  }

  public CollectionRun runSource(long sourceId) {
    return execute(source(sourceId), "MANUAL", false, true);
  }

  public CollectionRun testSource(long sourceId) {
    return execute(source(sourceId), "TEST", true, false);
  }

  public CollectionRun runDaily() {
    if (!dailyRunning.compareAndSet(false, true)) {
      throw new IllegalStateException("Daily collection is already running");
    }
    CollectionRun daily = runs.start(null, "DAILY");
    try {
      int discovered = 0;
      int reviewItems = 0;
      int errors = 0;
      for (Source source : sources.listEnabled()) {
        CollectionRun child = execute(source, "DAILY_SOURCE", false, true);
        discovered += child.discoveredCount();
        reviewItems += child.reviewItemCount();
        errors += child.errorCount();
      }
      String status = errors == 0 ? "SUCCESS" : "PARTIAL_FAILURE";
      String summary = errors == 0 ? null : errors + " source run(s) failed";
      return runs.finish(daily.id(), status, discovered, reviewItems, errors, summary);
    } catch (RuntimeException error) {
      runs.finish(daily.id(), "FAILED", 0, 0, 1, message(error));
      throw error;
    } finally {
      dailyRunning.set(false);
    }
  }

  private CollectionRun execute(Source source, String trigger, boolean testMode, boolean persist) {
    CollectionRun run = runs.start(source.id(), trigger);
    try {
      policy.requireAllowed(source, testMode);
      CollectionEngine.Discovery discovery = engine.discover(source, persist);
      if (discovery.notModified()) {
        sources.recordRun(source.id(), "NOT_MODIFIED", discovery.etag(), discovery.lastModified());
        return runs.finish(run.id(), "NOT_MODIFIED", 0, 0, 0, null);
      }
      int reviewItems = persist
          ? persistence.store(source.id(), run.id(), discovery.candidates()) : 0;
      sources.recordRun(source.id(), "SUCCESS", discovery.etag(), discovery.lastModified());
      return runs.finish(run.id(), "SUCCESS", discovery.candidates().size(), reviewItems, 0, null);
    } catch (CollectionException error) {
      sources.recordRun(source.id(), error.status(), null, null);
      return runs.finish(run.id(), error.status(), 0, 0, 1, message(error));
    } catch (RuntimeException error) {
      sources.recordRun(source.id(), "FAILED", null, null);
      return runs.finish(run.id(), "FAILED", 0, 0, 1, message(error));
    }
  }

  private Source source(long id) {
    return sources.find(id).orElseThrow(() -> new MissingResourceException("Source not found"));
  }

  private static String message(Throwable error) {
    return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
  }
}

package dev.recruitops;

import static org.junit.jupiter.api.Assertions.*;

import dev.recruitops.application.BrowserTaskService;
import dev.recruitops.domain.BrowserTask;
import dev.recruitops.infrastructure.SafeHttpFetcher;
import dev.recruitops.infrastructure.SourceRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties="recruitops.browser.fixtures-enabled=true")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class BrowserPacingTest extends CollectorTestSupport {
  @DynamicPropertySource static void fileDatabase(DynamicPropertyRegistry properties) {
    String url="jdbc:sqlite:target/browser-pacing-"+java.util.UUID.randomUUID()+".sqlite";
    properties.add("spring.datasource.url",()->url);
  }
  @Autowired BrowserTaskService tasks;
  @Autowired SourceRepository sources;
  @Autowired SafeHttpFetcher fetcher;

  BrowserTask lease(String path) {
    long source=insertSource(path,"MANUAL",true,"ALLOWED",Instant.now().toString(),5,"{\"row\":\"article\"}","{}");
    tasks.approveSource(source,"fixture");
    return tasks.claim(tasks.create(source,0,1,4000).id(),"worker-"+source);
  }
  long pace(BrowserTask task) { return tasks.pace(task.id(),task.workerId(),task.leaseToken()); }

  @Test void robotsRecheckAndFinalBrowserReadShareTheCrawlDelay() {
    AtomicLong robotsAt=new AtomicLong();
    route("/robots.txt",exchange->{robotsAt.set(System.currentTimeMillis());
      respond(exchange,200,"text/plain","User-agent: *\nAllow: /\nCrawl-delay: 1");});
    var task=lease("/table");
    tasks.checkRequest(task.id(),task.workerId(),task.leaseToken(),origin+"/table");
    long readAt=pace(task);
    assertTrue(readAt-robotsAt.get()>=900,"Final browser read must wait after robots, gap="+(readAt-robotsAt.get()));
  }

  @Test void concurrentSourcesOnOneOriginHaveDistinctReadSlots() throws Exception {
    route("/robots.txt",200,"text/plain",()->"User-agent: *\nAllow: /",null);
    var left=lease("/table-a");var right=lease("/table-b");
    var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(6);
    try {
      var futures=new ArrayList<java.util.concurrent.Future<Long>>();
      for(int i=0;i<6;i++) {var task=i%2==0?left:right;
        futures.add(executor.submit(()->{start.await();return pace(task);}));}
      start.countDown();var slots=new ArrayList<Long>();
      for(var future:futures)slots.add(future.get(10,TimeUnit.SECONDS));
      slots.sort(Long::compare);
      for(int i=1;i<slots.size();i++)assertTrue(slots.get(i)-slots.get(i-1)>=100,"Same-origin slots overlap: "+slots);
    } finally {executor.shutdownNow();}
  }

  @Test void ordinaryHttpCollectionWaitsForBrowserReservations() {
    route("/robots.txt",200,"text/plain",()->"User-agent: *\nAllow: /",null);
    AtomicLong requestedAt=new AtomicLong();
    route("/ordinary",exchange->{requestedAt.set(System.currentTimeMillis());respond(exchange,200,"text/plain","ok");});
    var task=lease("/table");pace(task);long browserAt=pace(task);
    var source=sources.find(task.sourceId()).orElseThrow();
    fetcher.fetch(source,URI.create(origin+"/ordinary"),false,0,false);
    assertTrue(requestedAt.get()-browserAt>=90,"HTTP read ignored browser reservations");
  }

  @Test void leaseRevalidationIsNetworkFreeAndRejectsRevocationAndExpiry() {
    var reads=new java.util.concurrent.atomic.AtomicInteger();
    route("/robots.txt",200,"text/plain",()->"User-agent: *\nAllow: /",reads);
    var task=lease("/table"); int before=reads.get();
    tasks.checkLease(task.id(),task.workerId(),task.leaseToken());
    assertEquals(before,reads.get());
    assertThrows(IllegalStateException.class,()->tasks.checkLease(task.id(),task.workerId(),"stale-token"));
    tasks.revokeSource(task.sourceId());
    assertThrows(IllegalStateException.class,()->tasks.checkLease(task.id(),task.workerId(),task.leaseToken()));
    tasks.approveSource(task.sourceId(),"fixture");
    jdbc.update("UPDATE browser_tasks SET lease_expires_at=0 WHERE id=?",task.id());
    assertThrows(IllegalStateException.class,()->tasks.checkLease(task.id(),task.workerId(),task.leaseToken()));
    assertEquals(before,reads.get());
  }

  @Test void freshRobotsDelayRaisesAnExistingTasksResourceInterval() {
    var delay=new java.util.concurrent.atomic.AtomicInteger();
    route("/robots.txt",200,"text/plain",()->"User-agent: *\nAllow: /\nCrawl-delay: "+delay.get(),null);
    var task=lease("/table"); delay.set(2);
    tasks.checkRequest(task.id(),task.workerId(),task.leaseToken(),origin+"/table");
    long first=pace(task),second=pace(task);
    assertTrue(second-first>=2000);
  }
}

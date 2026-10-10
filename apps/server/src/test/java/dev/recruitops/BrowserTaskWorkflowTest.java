package dev.recruitops;

import static org.junit.jupiter.api.Assertions.*;
import dev.recruitops.application.BrowserTaskService;
import dev.recruitops.infrastructure.BrowserTaskRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties="recruitops.browser.fixtures-enabled=true")
class BrowserTaskWorkflowTest extends CollectorTestSupport {
  @Autowired BrowserTaskService tasks;
  @Autowired BrowserTaskRepository repository;
  @BeforeEach void clearManual() { jdbc.update("DELETE FROM manual_items"); }
  long approved() {
    route("/robots.txt",200,"text/plain",()->"User-agent: *\nAllow: /",null);
    long source=insertSource("/table","MANUAL",true,"ALLOWED",Instant.now().toString(),5,"{\"row\":\"tbody tr\"}","{}");
    tasks.approveSource(source,"fixture"); return source;
  }
  BrowserTaskService.Row row(String kind) {
    return new BrowserTaskService.Row(0,"row-1","role-1",kind,origin+"/table",Instant.now().toString(),
        "row-1 role-1 Acme Engineer Cloud 2027 Shanghai official-1 Build Java",Map.of("companyName","Acme","title","Engineer", "businessUnit","Cloud","recruitmentYear",2027,"city","Shanghai","officialJobId","official-1","jd","Build Java"));
  }
  @Test void completionIsFencedPendingOnlyAndReplayIsIdempotent() {
    long source=approved(); var task=tasks.create(source,0,1,4000); var lease=tasks.claim(task.id(),"worker-a");
    assertThrows(IllegalStateException.class,()->tasks.claim(task.id(),"worker-b"));
    var body=new BrowserTaskService.Completion("worker-a",lease.leaseToken(),List.of(row("ROLE")));
    assertThrows(IllegalStateException.class,()->tasks.complete(task.id(),new BrowserTaskService.Completion("worker-b",lease.leaseToken(),body.rows())));
    tasks.complete(task.id(),body); tasks.complete(task.id(),body);
    assertEquals(1,count("review_items")); assertEquals(0,count("jobs")); assertEquals(1,count("browser_ingestion_evidence"));
  }
  @Test void expiresAndPolicyRevocationBlockResults() {
    long source=approved(); var lease=tasks.claim(tasks.create(source,0,1,4000).id(),"worker-a");
    jdbc.update("UPDATE browser_tasks SET lease_expires_at=0 WHERE id=?",lease.id());
    assertThrows(IllegalStateException.class,()->tasks.complete(lease.id(),new BrowserTaskService.Completion("worker-a",lease.leaseToken(),List.of(row("ROLE")))));
    var fresh=tasks.claim(lease.id(),"worker-b"); repository.revoke(source);
    assertThrows(IllegalStateException.class,()->tasks.complete(fresh.id(),new BrowserTaskService.Completion("worker-b",fresh.leaseToken(),List.of(row("ROLE")))));
    assertEquals(0,count("review_items"));
  }
  @Test void companyLeadsRemainManualAndUntrustedFieldsAreRejected() {
    long source=approved(); var lease=tasks.claim(tasks.create(source,0,1,4000).id(),"worker");
    var bad=new BrowserTaskService.Row(0,"r","company","COMPANY",origin+"/table",Instant.now().toString(),"Acme",Map.of("companyName","Invented"));
    assertThrows(IllegalArgumentException.class,()->tasks.complete(lease.id(),new BrowserTaskService.Completion("worker",lease.leaseToken(),List.of(bad))));
    tasks.complete(lease.id(),new BrowserTaskService.Completion("worker",lease.leaseToken(),List.of(row("COMPANY"))));
    assertEquals(1,count("manual_items")); assertEquals(0,count("review_items")); assertEquals(0,count("jobs"));
  }
  @Test void unapprovedSourcesWrongOriginsAndBudgetsFailClosed() {
    long source=insertSource("/table","MANUAL",true,"ALLOWED",Instant.now().toString(),5,"{}","{}");
    assertThrows(IllegalStateException.class,()->tasks.create(source,0,1,4000));
    route("/robots.txt",200,"text/plain",()->"User-agent: *\nAllow: /",null);
    tasks.approveSource(source,"fixture");
    assertThrows(IllegalArgumentException.class,()->tasks.create(source,0,100,4000));
    var lease=tasks.claim(tasks.create(source,0,1,4000).id(),"worker");
    var valid=row("ROLE");
    var badOrigin=new BrowserTaskService.Row(0,valid.sourceRowId(),valid.itemDiscriminator(),"ROLE","https://other.example/jobs",valid.capturedAt(),valid.visibleText(),valid.fields());
    assertThrows(IllegalArgumentException.class,()->tasks.complete(lease.id(),new BrowserTaskService.Completion("worker",lease.leaseToken(),List.of(badOrigin))));
    var oversized=new BrowserTaskService.Row(0,valid.sourceRowId(),valid.itemDiscriminator(),"ROLE",valid.sourceUrl(),valid.capturedAt(),"x".repeat(4001),valid.fields());
    assertThrows(IllegalArgumentException.class,()->tasks.complete(lease.id(),new BrowserTaskService.Completion("worker",lease.leaseToken(),List.of(oversized))));
    assertEquals("CLAIMED",repository.find(lease.id()).status());assertEquals(0,count("browser_ingestion_evidence"));
  }
  @Test void sourceStopRevokesPeerWorkersAndConfigurationChangeInvalidatesTasks() {
    long source=approved();var left=tasks.claim(tasks.create(source,0,1,4000).id(),"left");
    var right=tasks.claim(tasks.create(source,0,1,4000).id(),"right");
    tasks.stop(left.id(),"left",left.leaseToken(),"HTTP access barrier 403");
    assertThrows(IllegalStateException.class,()->tasks.complete(right.id(),new BrowserTaskService.Completion("right",right.leaseToken(),List.of(row("ROLE")))));
    tasks.approveSource(source,"fixture");jdbc.update("UPDATE sources SET config_version=config_version+1 WHERE id=?",source);
    assertThrows(IllegalStateException.class,()->tasks.complete(right.id(),new BrowserTaskService.Completion("right",right.leaseToken(),List.of(row("ROLE")))));
  }
  @Test void everyRequestChecksRobotsWithoutFetchingSubmittedUrl() {
    var sourceReads=new java.util.concurrent.atomic.AtomicInteger();
    route("/robots.txt",200,"text/plain",()->"User-agent: *\nAllow: /\nDisallow: /denied",null);
    route("/allowed-xhr",200,"text/plain",()->"allowed",sourceReads);
    route("/denied-xhr",200,"text/plain",()->"denied",sourceReads);
    long source=insertSource("/table","MANUAL",true,"ALLOWED",Instant.now().toString(),5,"{\"row\":\"article\"}","{}");
    tasks.approveSource(source,"fixture");var lease=tasks.claim(tasks.create(source,0,1,4000).id(),"worker");
    tasks.checkRequest(lease.id(),"worker",lease.leaseToken(),origin+"/allowed-xhr");
    assertThrows(RuntimeException.class,()->tasks.checkRequest(lease.id(),"worker",lease.leaseToken(),origin+"/denied-xhr"));
    assertThrows(IllegalArgumentException.class,()->tasks.checkRequest(lease.id(),"worker",lease.leaseToken(),"https://unapproved.example/path"));
    assertEquals(0,sourceReads.get());
    repository.revoke(source);
    assertThrows(IllegalStateException.class,()->tasks.checkRequest(lease.id(),"worker",lease.leaseToken(),origin+"/allowed-xhr"));
  }
}

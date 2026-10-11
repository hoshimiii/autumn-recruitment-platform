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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties="recruitops.browser.fixtures-enabled=true")
@AutoConfigureMockMvc
class BrowserTaskWorkflowTest extends CollectorTestSupport {
  @Autowired BrowserTaskService tasks;
  @Autowired BrowserTaskRepository repository;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
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
  @Test void ordinaryFailurePreservesApprovalAndPeerLeaseAndRequiresCurrentCredentials() throws Exception {
    long source=approved();var left=tasks.claim(tasks.create(source,0,1,4000).id(),"left");
    var right=tasks.claim(tasks.create(source,0,1,4000).id(),"right");
    mvc.perform(post("/api/v1/browser-tasks/{id}/fail",left.id()).contentType("application/json")
        .content(json.writeValueAsString(Map.of("workerId","wrong","leaseToken",left.leaseToken(),"reason","Provider timeout"))))
        .andExpect(status().isConflict());
    mvc.perform(post("/api/v1/browser-tasks/{id}/fail",left.id()).contentType("application/json")
        .content(json.writeValueAsString(Map.of("workerId","left","leaseToken",left.leaseToken(),"reason","Provider timeout"))))
        .andExpect(status().isOk());
    assertEquals("FAILED",repository.find(left.id()).status());
    assertEquals("CLAIMED",repository.find(right.id()).status());
    tasks.checkAccess(right.id(),"right",right.leaseToken());
    tasks.complete(right.id(),new BrowserTaskService.Completion("right",right.leaseToken(),List.of(row("ROLE"))));
    assertEquals("PENDING",tasks.create(source,0,1,4000).status());
  }
  @Test void resultsRequireEveryAssignedIndexExactlyOnceAndRejectAtomically() {
    long source=approved();var lease=tasks.claim(tasks.create(source,0,2,4000).id(),"worker");
    var duplicate=new BrowserTaskService.Row(0,"row-2","role-2","ROLE",origin+"/table",Instant.now().toString(),
        "row-2 role-2 Acme Engineer official-2 Build Java",Map.of("companyName","Acme","title","Engineer","officialJobId","official-2","jd","Build Java"));
    for(var rows:List.of(List.<BrowserTaskService.Row>of(),List.of(row("ROLE")),List.of(row("ROLE"),duplicate))) {
      assertThrows(IllegalArgumentException.class,()->tasks.complete(lease.id(),new BrowserTaskService.Completion("worker",lease.leaseToken(),rows)));
      assertEquals("CLAIMED",repository.find(lease.id()).status());
      assertEquals(0,count("browser_ingestion_evidence"));assertEquals(0,count("review_items"));assertEquals(0,count("manual_items"));
    }
    var second=new BrowserTaskService.Row(1,duplicate.sourceRowId(),duplicate.itemDiscriminator(),duplicate.kind(),duplicate.sourceUrl(),duplicate.capturedAt(),duplicate.visibleText(),duplicate.fields());
    var full=new BrowserTaskService.Completion("worker",lease.leaseToken(),List.of(second,row("ROLE")));
    tasks.complete(lease.id(),full);tasks.complete(lease.id(),full);
    assertEquals(2,count("review_items"));assertEquals(2,count("browser_ingestion_evidence"));
  }
  @Test void listNeverExposesCredentialsWhileClaimResponseRemainsUsable() throws Exception {
    long source=approved();var task=tasks.create(source,0,1,4000);
    var claimed=mvc.perform(post("/api/v1/browser-tasks/{id}/claim",task.id()).contentType("application/json")
        .content("{\"workerId\":\"private-worker\"}")).andExpect(status().isOk()).andReturn();
    var claim=json.readTree(claimed.getResponse().getContentAsString());
    assertEquals("private-worker",claim.path("workerId").asText());assertFalse(claim.path("leaseToken").asText().isBlank());
    var response=mvc.perform(get("/api/v1/browser-tasks")).andExpect(status().isOk()).andReturn();
    var listing=json.readTree(response.getResponse().getContentAsString()).path("items").get(0);
    assertFalse(listing.has("leaseToken"));assertFalse(listing.has("workerId"));
    assertEquals(task.id(),listing.path("id").asLong());assertEquals("CLAIMED",listing.path("status").asText());
    tasks.complete(task.id(),new BrowserTaskService.Completion("private-worker",claim.path("leaseToken").asText(),List.of(row("ROLE"))));
  }
  @Test void requestChecksDistinguishConfirmedBarriersFromRobotsOutages() throws Exception {
    long source=approved();var lease=tasks.claim(tasks.create(source,0,1,4000).id(),"worker");
    server.removeContext("/robots.txt");route("/robots.txt",503,"text/plain",()->"Temporary outage",null);
    String body=json.writeValueAsString(Map.of("workerId","worker","leaseToken",lease.leaseToken(),"requestUrl",origin+"/table"));
    mvc.perform(post("/api/v1/browser-tasks/{id}/check-request",lease.id()).contentType("application/json").content(body))
        .andExpect(status().isConflict());
    assertEquals(0,jdbc.queryForObject("SELECT revoked FROM browser_source_policies WHERE source_id=?",Integer.class,source));
    server.removeContext("/robots.txt");route("/robots.txt",200,"text/plain",()->"User-agent: *\nDisallow: /table",null);
    var denied=mvc.perform(post("/api/v1/browser-tasks/{id}/check-request",lease.id()).contentType("application/json").content(body))
        .andExpect(status().isForbidden()).andReturn();
    assertEquals("browser_access_barrier",json.readTree(denied.getResponse().getContentAsString()).path("error").path("code").asText());
    server.removeContext("/robots.txt");route("/robots.txt",403,"text/plain",()->"Access denied",null);
    mvc.perform(post("/api/v1/browser-tasks/{id}/check-request",lease.id()).contentType("application/json").content(body))
        .andExpect(status().isForbidden());
  }
}

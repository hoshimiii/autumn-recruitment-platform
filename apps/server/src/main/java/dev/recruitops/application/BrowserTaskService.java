package dev.recruitops.application;

import dev.recruitops.domain.BrowserTask;
import dev.recruitops.domain.Source;
import dev.recruitops.infrastructure.BrowserTaskRepository;
import dev.recruitops.infrastructure.JsonValues;
import dev.recruitops.infrastructure.ManualItemRepository;
import dev.recruitops.infrastructure.MissingResourceException;
import dev.recruitops.infrastructure.RobotsPolicy;
import dev.recruitops.infrastructure.SourceRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrowserTaskService {
  private final BrowserTaskRepository tasks; private final SourceRepository sources; private final JdbcTemplate jdbc;
  private final CandidateNormalizer normalizer; private final JsonValues json; private final ManualItemRepository manuals;
  private final RobotsPolicy robots; private final boolean fixtures;
  public BrowserTaskService(BrowserTaskRepository tasks,SourceRepository sources,JdbcTemplate jdbc,
      CandidateNormalizer normalizer,JsonValues json,ManualItemRepository manuals,RobotsPolicy robots,
      @Value("${recruitops.browser.fixtures-enabled:false}") boolean fixtures) {
    this.tasks=tasks;this.sources=sources;this.jdbc=jdbc;this.normalizer=normalizer;this.json=json;
    this.manuals=manuals;this.robots=robots;this.fixtures=fixtures;
  }
  @Transactional @PreAuthorize("@operationAuthorizer.can('ADMIN')")
  public void approveSource(long id,String adapter) {
    jdbc.update("UPDATE browser_source_policies SET revoked=revoked WHERE source_id=?",id);
    Source source=source(id); requireSource(source);
    if(adapter==null || !List.of("fixture","sub2api").contains(adapter)) throw new IllegalArgumentException("Unknown browser adapter");
    if("fixture".equals(adapter) && (!fixtures || !loopback(URI.create(source.baseUrl()).getHost())))
      throw new IllegalArgumentException("Fixture adapter requires explicit local fixture runtime");
    tasks.approve(source,adapter);
  }
  @Transactional @PreAuthorize("@operationAuthorizer.can('ADMIN')")
  public void revokeSource(long id) { tasks.revoke(id); }
  @PreAuthorize("@operationAuthorizer.can('COLLECTOR')")
  public List<BrowserTask> list() { return tasks.list(); }
  @PreAuthorize("@operationAuthorizer.can('COLLECTOR')")
  public BrowserTask create(long sourceId,int start,int end,int maxChars) {
    Source s=source(sourceId); requireSource(s); String adapter=tasks.requirePolicy(s);
    if(start<0 || end<=start || end-start>Math.min(100,s.maxItems()) || maxChars<100 || maxChars>50_000)
      throw new IllegalArgumentException("Invalid row range/budget");
    var access=robots.check(s);access.requireAllowed(URI.create(s.listUrl()));
    int delay=(int)Math.max(s.minIntervalSeconds(),(access.crawlDelayMillis()+999)/1000);
    return tasks.create(s,start,end,maxChars,adapter,SourcePolicy.origin(URI.create(s.baseUrl())),delay,fixtures&&loopback(URI.create(s.baseUrl()).getHost()));
  }
  @Transactional @PreAuthorize("@operationAuthorizer.can('COLLECTOR')")
  public BrowserTask claim(long id,String worker) {
    requireId(worker); BrowserTask task=tasks.claim(id,worker); current(task); return task;
  }
  @PreAuthorize("@operationAuthorizer.can('COLLECTOR')")
  public void checkAccess(long id,String worker,String token) {
    checkRequest(id,worker,token,tasks.find(id).listUrl());
  }
  @PreAuthorize("@operationAuthorizer.can('COLLECTOR')")
  public void checkRequest(long id,String worker,String token,String requestUrl) {
    requireId(worker);requireId(token);
    if(requestUrl==null || requestUrl.length()>4096)throw new IllegalArgumentException("Invalid source request URL");
    BrowserTask task=leased(id,worker,token);Source source=current(task);
    URI url=SourcePolicy.valid("request_url",requestUrl).normalize();
    if(!SourcePolicy.origin(url).equals(task.origin()) || url.toString().matches("(?i).*(/login|/signin|captcha).*"))throw new IllegalArgumentException("Source request origin/barrier rejected");
    // The submitted URL is checked against rules; only the configured robots URL is fetched.
    robots.check(source).requireAllowed(url);
    current(leased(id,worker,token));
  }
  @Transactional @PreAuthorize("@operationAuthorizer.can('COLLECTOR')")
  public long pace(long id,String worker,String token) {
    long time=tasks.pace(id,worker,token); current(tasks.find(id));
    return time;
  }
  @Transactional @PreAuthorize("@operationAuthorizer.can('COLLECTOR')")
  public BrowserTask stop(long id,String worker,String token,String reason) {
    if(reason==null || reason.length()>300) throw new IllegalArgumentException("Invalid stop reason");
    tasks.stop(id,worker,token,reason);BrowserTask task=tasks.find(id);tasks.revoke(task.sourceId());return task;
  }
  @Transactional @PreAuthorize("@operationAuthorizer.can('COLLECTOR')")
  public BrowserTask complete(long id,Completion result) {
    if(result==null || result.rows()==null || result.rows().size()>100) throw new IllegalArgumentException("Invalid browser result");
    String serialized=json.write(result); if(serialized.length()>100_000) throw new IllegalArgumentException("Result too large");
    String hash=hash(serialized); boolean changed=tasks.fence(id,result.workerId(),result.leaseToken(),hash);
    BrowserTask task=tasks.find(id); Source source=current(task); if(!changed) return task;
    if(result.rows().size()>task.maxRows()) throw new IllegalArgumentException("Too many rows");
    HashSet<String> identities=new HashSet<>(); int total=0;
    for(Row row:result.rows()) {
      validate(task,row); total+=row.visibleText().length();
      if(total>task.maxChars() || !identities.add(row.sourceRowId()+"\n"+row.itemDiscriminator())) throw new IllegalArgumentException("Duplicate identity/visible text budget exceeded");
      ingest(task,source,row);
    }
    tasks.finish(id,hash); return tasks.find(id);
  }
  private void ingest(BrowserTask task,Source source,Row row) {
    Map<String,Object> raw=new java.util.LinkedHashMap<>(row.fields());
    raw.put("sourceUrl",row.sourceUrl());raw.put("sourceRowId",row.sourceRowId());raw.put("itemDiscriminator",row.itemDiscriminator());
    raw.put("sourceTrust",source.trustLevel());raw.put("companyCategory",source.companyCategory());
    String version=hash(json.write(raw));
    int inserted=jdbc.update("""
        INSERT OR IGNORE INTO browser_ingestion_evidence(task_id,source_id,source_row_id,item_discriminator,content_hash,evidence_json)
        VALUES (?,?,?,?,?,?)
        """,task.id(),task.sourceId(),row.sourceRowId(),row.itemDiscriminator(),version,json.write(Map.of("taskId",task.id(),"workerId",task.workerId(),"row",row)));
    if(inserted==0) return;
    Long manualId=null,reviewId=null;
    if("ROLE".equals(row.kind())) {
      var candidate=normalizer.normalize(source,raw);
      jdbc.update("""
          INSERT OR IGNORE INTO review_items(source_id,candidate_payload,normalized_payload,dedupe_key,content_hash,confidence,status)
          VALUES (?,?,?,?,?,1.0,'PENDING_REVIEW')
          """,source.id(),json.write(Map.of("fields",raw,"evidence",row,"taskId",task.id(),"workerId",task.workerId())),json.write(candidate.normalizedPayload()),candidate.dedupeKey(),candidate.contentHash());
      reviewId=jdbc.queryForObject("SELECT id FROM review_items WHERE source_id=? AND dedupe_key=? AND content_hash=?",Long.class,source.id(),candidate.dedupeKey(),candidate.contentHash());
    } else {
      manualId=manuals.create(new ManualItemRepository.ManualInput(source.name(),String.valueOf(raw.get("companyName")),null,
          List.of(),row.sourceUrl(),List.of(),null,"核验公司线索与独立岗位",false,false,null,null,0,"PENDING",json.write(Map.of("fields",raw,"evidence",row,"taskId",task.id(),"workerId",task.workerId())))).id();
    }
    jdbc.update("UPDATE browser_ingestion_evidence SET manual_item_id=?,review_item_id=? WHERE source_id=? AND source_row_id=? AND item_discriminator=? AND content_hash=?",manualId,reviewId,source.id(),row.sourceRowId(),row.itemDiscriminator(),version);
  }
  private void validate(BrowserTask task,Row row) {
    if(row==null || row.rowIndex()<task.rowStart() || row.rowIndex()>=task.rowEnd() || row.fields()==null || row.fields().size()>20)
      throw new IllegalArgumentException("Row outside task range/invalid fields");
    requireId(row.sourceRowId());requireId(row.itemDiscriminator());
    if(!("ROLE".equals(row.kind()) || "COMPANY".equals(row.kind())) || row.visibleText()==null || row.visibleText().isBlank() || row.visibleText().length()>task.maxChars()) throw new IllegalArgumentException("Invalid row evidence");
    if(!row.visibleText().contains(row.sourceRowId()) || !row.visibleText().contains(row.itemDiscriminator())) throw new IllegalArgumentException("Row identity absent from visible evidence");
    URI url=SourcePolicy.valid("source_url",row.sourceUrl());
    if(!SourcePolicy.origin(url).equals(task.origin())) throw new IllegalArgumentException("Unapproved evidence origin");
    try { long captured=Instant.parse(row.capturedAt()).toEpochMilli(); if(captured>System.currentTimeMillis()+5000 || captured<task.leaseExpiresAt()-120_000) throw new IllegalArgumentException("Evidence capture outside lease"); }
    catch(RuntimeException error) { throw new IllegalArgumentException("Invalid evidence capture timestamp",error); }
    var fields=List.of("companyName","title","businessUnit","recruitmentYear","city","officialJobId","jd","requirements","applyUrl","jobType","jobFamily","deadline");
    for(var field:row.fields().entrySet()) {
      if(!fields.contains(field.getKey())) throw new IllegalArgumentException("Unsupported worker field: "+field.getKey());
      if(field.getValue()!=null && !(field.getValue() instanceof String) && !("recruitmentYear".equals(field.getKey()) && field.getValue() instanceof Integer)) throw new IllegalArgumentException("Worker fields must be scalar text/integer cohort");
      if(field.getValue()!=null && !String.valueOf(field.getValue()).isBlank() && !row.visibleText().contains(String.valueOf(field.getValue())))
        throw new IllegalArgumentException("Worker field absent from visible evidence: "+field.getKey());
    }
    if(!present(row.fields(),"companyName")) throw new IllegalArgumentException("Company evidence missing");
    if("ROLE".equals(row.kind()) && (!present(row.fields(),"title") || !present(row.fields(),"officialJobId") || !present(row.fields(),"jd"))) throw new IllegalArgumentException("Standalone role requires title, official ID and JD evidence");
  }
  private Source current(BrowserTask task) {
    Source s=source(task.sourceId());requireSource(s);String adapter=tasks.requirePolicy(s);
    if(s.configVersion()!=task.sourceVersion() || !s.termsConfirmedAt().equals(task.termsVersion()) || !adapter.equals(task.adapter())) throw new IllegalStateException("Task policy changed");
    return s;
  }
  private BrowserTask leased(long id,String worker,String token) {
    BrowserTask task=tasks.find(id);
    if(!"CLAIMED".equals(task.status()) || !worker.equals(task.workerId()) || !token.equals(task.leaseToken()) || task.leaseExpiresAt()<=System.currentTimeMillis())throw new IllegalStateException("Expired/stale lease");
    return task;
  }
  private Source source(long id) { return sources.find(id).orElseThrow(()->new MissingResourceException("Source not found")); }
  private void requireSource(Source s) {
    if(!s.enabled() || !"ALLOWED".equals(s.complianceStatus()) || s.termsConfirmedAt()==null || s.termsConfirmedAt().isBlank() || s.termsUrl()==null || s.termsUrl().isBlank()) throw new IllegalStateException("Source disabled or policy/terms unconfirmed");
    URI base=SourcePolicy.valid("base_url",s.baseUrl()),list=SourcePolicy.valid("list_url",s.listUrl());
    SourcePolicy.valid("terms_url",s.termsUrl());
    if(s.robotsUrl()!=null && !s.robotsUrl().isBlank() && !SourcePolicy.sameOrigin(base,SourcePolicy.valid("robots_url",s.robotsUrl()))) throw new IllegalArgumentException("Unapproved robots origin");
    if(!SourcePolicy.sameOrigin(base,list) || list.toString().matches("(?i).*(/login|/signin|captcha).*")) throw new IllegalArgumentException("Browser URL origin/barrier rejected");
  }
  private static void requireId(String value) { if(value==null || value.isBlank() || value.length()>200 || value.chars().anyMatch(c->c<32)) throw new IllegalArgumentException("Invalid worker/row identity"); }
  private static boolean present(Map<String,Object> fields,String key) { return fields.get(key)!=null && !String.valueOf(fields.get(key)).isBlank(); }
  private static boolean loopback(String host) { return List.of("127.0.0.1","localhost","::1").contains(host); }
  private static String hash(String text) {
    try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
    catch(Exception error) { throw new IllegalStateException(error); }
  }
  public record Completion(String workerId,String leaseToken,List<Row> rows) {}
  public record Row(int rowIndex,String sourceRowId,String itemDiscriminator,String kind,String sourceUrl,String capturedAt,String visibleText,Map<String,Object> fields) {}
}

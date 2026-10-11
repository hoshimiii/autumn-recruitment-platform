package dev.recruitops;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.application.CandidateNormalizer;
import dev.recruitops.application.ReviewService;
import dev.recruitops.infrastructure.JobRepository;
import dev.recruitops.infrastructure.JsonValues;
import dev.recruitops.infrastructure.ReviewRepository;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@SpringBootTest
class CanonicalIdentityUpgradeTest extends CollectorTestSupport {
  @Autowired ReviewService review;
  @Autowired ObjectMapper json;

  Map<String,Object> payload() {
    return new HashMap<>(Map.of("companyName","Acme", "companyCategory","INTERNET",
        "title","Engineer", "jobType","FULL_TIME", "city","Shanghai",
        "sourceUrl",origin+"/jobs", "sourceTrust","OFFICIAL", "jd","Original description"));
  }
  long source() { return insertSource("/jobs","HTML",true,"ALLOWED","2026-10-10T00:00:00Z",10,"{}","{}"); }
  String oldKey() { return "acme|engineer|shanghai|"+origin+"/jobs"; }
  long oldJob(long source, Map<String,Object> value) {
    return oldJob(jdbc,source,value);
  }
  long oldJob(JdbcTemplate database,long source,Map<String,Object> value) {
    database.update("""
        INSERT INTO jobs(company_type,company_name,title,job_type,city,status,source_id,
          source_url,source_trust_level,jd,dedupe_key,business_unit,campus_batch)
        VALUES ('INTERNET','Acme','Engineer','FULL_TIME','Shanghai','PUBLISHED',?,?,'OFFICIAL',?,?,?,?)
        """,source,origin+"/jobs",value.get("jd"),oldKey(),value.get("businessUnit"),value.get("recruitmentYear"));
    return database.queryForObject("SELECT id FROM jobs WHERE dedupe_key=?",Long.class,oldKey());
  }
  long pending(long source, Map<String,Object> value) throws Exception {
    return pending(jdbc,source,value);
  }
  long pending(JdbcTemplate database,long source,Map<String,Object> value) throws Exception {
    String encoded=json.writeValueAsString(value);
    database.update("INSERT INTO review_items(source_id,candidate_payload,normalized_payload,dedupe_key,status) VALUES (?,?,?,?,'PENDING_REVIEW')",
        source,encoded,encoded,oldKey());
    return database.queryForObject("SELECT id FROM review_items ORDER BY id DESC LIMIT 1",Long.class);
  }
  @Test void unchangedLegacyJobKeepsIdAndHistoryAndAdoptsNewKey() throws Exception {
    long source=source(); var value=payload(); long existing=oldJob(source,value);
    jdbc.update("INSERT INTO job_snapshots(job_id,source_id,source_url,content_hash,parsed_payload) VALUES (?,?,?,'old-hash',?)",
        existing,source,origin+"/jobs",json.writeValueAsString(value));
    value.put("jd","Updated description");
    var saved=review.approve(pending(source,value),null);
    assertEquals(existing,saved.id()); assertEquals(1,count("jobs"));
    assertEquals("Updated description",saved.jd()); assertEquals(2,count("job_snapshots"));
    assertEquals("UPDATED",jdbc.queryForObject("SELECT action FROM audit_logs ORDER BY id DESC LIMIT 1",String.class));
    assertEquals(CandidateNormalizer.identity(source,json.valueToTree(value)),
        jdbc.queryForObject("SELECT dedupe_key FROM jobs WHERE id=?",String.class,existing));
    value.put("jd","Third description");
    assertEquals(existing,review.approve(pending(source,value),null).id());
    assertEquals(1,count("jobs")); assertEquals(3,count("job_snapshots"));
  }
  @Test void matchingKnownDepartmentAndCohortCanReuseLegacyJob() throws Exception {
    long source=source(); var value=payload(); value.put("businessUnit","Cloud"); value.put("recruitmentYear",2027);
    long existing=oldJob(source,value);
    assertEquals(existing,review.approve(pending(source,value),null).id()); assertEquals(1,count("jobs"));
  }
  @Test void missingLegacyIdentityIsNeverAWildcardAndSourcesStaySeparate() throws Exception {
    long source=source(), otherSource=source(); var value=payload(); long existing=oldJob(source,value);
    for(var identity:Map.of("businessUnit","Cloud","recruitmentYear",2027,"officialJobId","official-1","sourceRowId","row-1").entrySet()) {
      var changed=payload(); changed.put(identity.getKey(),identity.getValue());
      if(identity.getKey().equals("sourceRowId"))changed.put("itemDiscriminator","role-1");
      assertNotEquals(existing,review.approve(pending(source,changed),null).id());
    }
    assertNotEquals(existing,review.approve(pending(otherSource,value),null).id());
    assertEquals(6,count("jobs"));
    assertEquals(oldKey(),jdbc.queryForObject("SELECT dedupe_key FROM jobs WHERE id=?",String.class,existing));
    assertEquals("Original description",jdbc.queryForObject("SELECT jd FROM jobs WHERE id=?",String.class,existing));
  }
  @Test void actualV4ToCurrentMigrationKeepsPublishedJobOnApproval(@TempDir Path temporary) throws Exception {
    String url="jdbc:sqlite:"+temporary.resolve("legacy-upgrade.sqlite");
    var dataSource=new DriverManagerDataSource(url);
    Flyway.configure().dataSource(dataSource).target("4").load().migrate();
    var database=new JdbcTemplate(dataSource);
    database.update("""
        INSERT INTO sources(name,type,base_url,adapter_type,mode,compliance_status,trust_level,scope)
        VALUES ('Upgrade fixture','COMPANY',?,'JSON','JSON','ALLOWED','OFFICIAL','INTERNET')
        """,origin);
    long source=database.queryForObject("SELECT id FROM sources WHERE name='Upgrade fixture'",Long.class);
    var value=payload(); long existing=oldJob(database,source,value);
    Flyway.configure().dataSource(dataSource).load().migrate();
    assertEquals(oldKey(),database.queryForObject("SELECT dedupe_key FROM jobs WHERE id=?",String.class,existing));
    var codec=new JsonValues(json);
    var upgradedReview=new ReviewService(new ReviewRepository(database,codec),new JobRepository(database,codec),codec);
    assertEquals(existing,upgradedReview.approve(pending(database,source,value),null).id());
    assertEquals(1,database.queryForObject("SELECT COUNT(*) FROM jobs",Integer.class));
    assertEquals(CandidateNormalizer.identity(source,json.valueToTree(value)),
        database.queryForObject("SELECT dedupe_key FROM jobs WHERE id=?",String.class,existing));
  }
}

package dev.recruitops;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.application.JobExportService;
import dev.recruitops.application.ReviewService;
import dev.recruitops.infrastructure.JobRepository;
import dev.recruitops.mcp.McpJobToolRegistry;
import java.io.ByteArrayInputStream;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CanonicalPublicationTest extends CollectorTestSupport {
  @Autowired ReviewService review;
  @Autowired ObjectMapper json;
  @Autowired JobExportService exports;
  @Autowired McpJobToolRegistry mcp;
  JobRepository.JobFilter filter=new JobRepository.JobFilter(null,null,null,null,null,null,null,null,null,null,null,1,100);
  Map<String,Object> payload() { return new java.util.HashMap<>(Map.of("companyName","Acme","companyCategory","INTERNET",
      "title","Engineer","jobType","FULL_TIME","sourceUrl",origin+"/jobs","sourceTrust","OFFICIAL","businessUnit","Cloud","recruitmentYear",2027,"city","Shanghai")); }
  long pending(long source,Map<String,Object> value) throws Exception {
    String text=json.writeValueAsString(value);
    jdbc.update("INSERT INTO review_items(source_id,candidate_payload,normalized_payload,dedupe_key,status) VALUES (?,?,?,'stale-original-identity','PENDING_REVIEW')",source,text,text);
    return jdbc.queryForObject("SELECT last_insert_rowid()",Long.class);
  }
  @Test void editedIdentityAndOptionalFieldsReachPublicInterfaces() throws Exception {
    long source=insertSource("/jobs","HTML",true,"ALLOWED","2026-10-10T00:00:00Z",10,"{}","{}");
    var first=review.approve(pending(source,payload()),null);
    var changed=payload();changed.put("businessUnit","Platform");
    var second=review.approve(pending(source,payload()),json.valueToTree(changed));
    assertNotEquals(first.id(),second.id());assertEquals("Platform",second.businessUnit());assertEquals(2027,second.recruitmentYear());
    assertTrue(mcp.getJson(Map.of("id",second.id())).contains("\"businessUnit\":\"Platform\""));
    assertTrue(mcp.searchJson(Map.of()).contains("\"recruitmentYear\":2027"));
    assertTrue(exports.markdown(filter).contains("businessUnit | recruitmentYear"));
    try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(exports.xlsx(filter)))) {
      assertEquals("businessUnit",workbook.getSheetAt(0).getRow(0).getCell(18).getStringCellValue());
      assertEquals("2027",workbook.getSheetAt(0).getRow(1).getCell(19).getStringCellValue());
    }
  }
  @Test void sameReplayRowWithConflictingOfficialJobIdPreservesPublishedJob() throws Exception {
    long source=insertSource("/jobs","HTML",true,"ALLOWED","2026-10-10T00:00:00Z",10,"{}","{}");
    var value=payload();value.put("sourceRowId","row-1");value.put("itemDiscriminator","role");value.put("officialJobId","official-1");
    var published=review.approve(pending(source,value),null);
    value.put("officialJobId","official-2");long candidate=pending(source,value);
    assertThrows(IllegalStateException.class,()->review.approve(candidate,null));
    assertEquals("official-1",jdbc.queryForObject("SELECT official_job_id FROM jobs WHERE id=?",String.class,published.id()));
    assertEquals("PENDING_REVIEW",jdbc.queryForObject("SELECT status FROM review_items WHERE id=?",String.class,candidate));
  }
}

package dev.recruitops;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.application.CandidateNormalizer;
import dev.recruitops.domain.Source;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalIdentityTest {
  final CandidateNormalizer normalizer = new CandidateNormalizer(new ObjectMapper());
  final Source source = new Source(12, "Fixture", "COMPANY", "https://example.com", "https://example.com/jobs",
      "HTML", "HTML", "ALLOWED", "OFFICIAL", "INTERNET", true, "", "UTC", 0, 1, 10,
      "{}", "{}", "https://example.com/terms", "2026-10-10", null, 5, null, null, null, null, false, null, null, 1);
  Map<String,Object> raw() { return new HashMap<>(Map.of("companyName","Acme", "title","Engineer",
      "sourceUrl","https://example.com/jobs", "sourceRowId","row-1", "itemDiscriminator","role-1")); }
  @Test void optionalFieldsAndReplayIdentityAreIndependentOfContent() {
    var raw = raw(); var first = normalizer.normalize(source, raw);
    assertTrue(first.normalizedPayload().path("businessUnit").isNull());
    assertTrue(first.normalizedPayload().path("recruitmentYear").isNull());
    raw.put("businessUnit", " Cloud "); raw.put("recruitmentYear", 2027);
    var second = normalizer.normalize(source, raw);
    assertEquals(first.dedupeKey(), second.dedupeKey());
    assertNotEquals(first.contentHash(), second.contentHash());
    assertEquals(2027, second.normalizedPayload().path("recruitmentYear").asInt());
    raw.put("itemDiscriminator", "role-2");
    assertNotEquals(second.dedupeKey(), normalizer.normalize(source,raw).dedupeKey());
  }
  @Test void unknownRowsAndDifferentOfficialIdsNeverCollapse() {
    var raw = raw(); raw.remove("sourceRowId"); raw.remove("itemDiscriminator");
    var first = normalizer.normalize(source,raw);
    raw.put("city", "Beijing"); assertNotEquals(first.dedupeKey(),normalizer.normalize(source,raw).dedupeKey());
    raw.put("officialJobId", "official-2");
    var second = normalizer.normalize(source,raw);
    raw.put("officialJobId", "official-3");
    assertNotEquals(second.dedupeKey(),normalizer.normalize(source,raw).dedupeKey());
  }
  @Test void rejectsInvalidCohort() {
    var raw = raw(); raw.put("recruitmentYear", "tomorrow");
    assertThrows(RuntimeException.class, () -> normalizer.normalize(source,raw));
  }
}

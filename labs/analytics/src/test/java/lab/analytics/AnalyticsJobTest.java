package lab.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class AnalyticsJobTest {
    @Test
    public void aggregatesByCohortDepartmentAndJobFamily() {
        assertEquals(Map.of(
                "2026|Design|Product", 1,
                "2027|Design|Product", 1,
                "2027|Engineering|Backend", 2),
                AnalyticsJob.aggregate(List.of(
                        "SYN-001,2027,Engineering,Backend",
                        "SYN-002,2027,Engineering,Backend",
                        "SYN-003,2026,Design,Product",
                        "SYN-004,2027,Design,Product")));
    }

    @Test
    public void rejectsMalformedRowsAndUnsafeKeys() {
        assertThrows(IllegalArgumentException.class, () -> AnalyticsJob.parseRow("SYN-001,2027,Eng|ineering,Backend"));
        assertThrows(IllegalArgumentException.class, () -> AnalyticsJob.parseRow("SYN-001,2027,Engineering"));
        assertThrows(IllegalArgumentException.class, () -> AnalyticsJob.parseRow("SYN-001,unknown,Engineering,Backend"));
    }

    @Test
    public void rejectsDuplicateSyntheticIds() {
        assertThrows(IllegalArgumentException.class, () -> AnalyticsJob.aggregate(List.of(
                "SYN-001,2027,Engineering,Backend",
                "SYN-001,2027,Design,Product")));
    }
}

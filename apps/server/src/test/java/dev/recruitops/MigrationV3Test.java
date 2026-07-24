package dev.recruitops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationV3Test {
  @TempDir Path temporaryDirectory;

  @Test
  void migratesCollectorConfigurationAndSafeSeedSources() {
    SQLiteDataSource dataSource = new SQLiteDataSource();
    dataSource.setUrl("jdbc:sqlite:" + temporaryDirectory.resolve("collector-test.sqlite"));
    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);

    assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM sources", Integer.class));
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sources WHERE enabled <> 0", Integer.class));
    assertEquals(0, jdbc.queryForObject(
        "SELECT COUNT(*) FROM sources WHERE terms_confirmed_at IS NOT NULL", Integer.class));
    assertEquals(5, jdbc.queryForObject(
        "SELECT COUNT(*) FROM sources WHERE ai_enabled = 0", Integer.class));
    assertEquals(5, jdbc.queryForObject("""
        SELECT COUNT(*) FROM sources WHERE name IN ('Tencent Careers JSON',
        'State-owned Assets Supervision Recruitment Index', 'Shandong Rural Credit Recruitment',
        'Alibaba Campus Recruitment', 'Huawei Campus Recruitment')
        """, Integer.class));

    jdbc.update("""
        INSERT INTO sources (name,type,base_url,adapter_type,mode,compliance_status,trust_level,scope)
        VALUES ('Defaults','COMPANY','https://example.com','JSON','JSON','UNKNOWN','OFFICIAL','OTHER')
        """);
    assertEquals(5, jdbc.queryForObject(
        "SELECT min_interval_seconds FROM sources WHERE name='Defaults'", Integer.class));
    assertEquals(1, jdbc.queryForObject(
        "SELECT max_pages FROM sources WHERE name='Defaults'", Integer.class));
    assertEquals(50, jdbc.queryForObject(
        "SELECT max_items FROM sources WHERE name='Defaults'", Integer.class));
    assertEquals(1, jdbc.queryForObject("""
        SELECT COUNT(*) FROM pragma_table_info('review_items') WHERE name='content_hash'
        """, Integer.class));
  }
}

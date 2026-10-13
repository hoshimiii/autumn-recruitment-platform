package dev.recruitops;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

class OriginPacingMigrationTest {
  @Test void upgradeKeepsLatestSharedOriginReservationAndSurvivesReopen(@TempDir Path temporary) {
    String url = "jdbc:sqlite:" + temporary.resolve("upgrade.sqlite");
    var source = new SQLiteDataSource(); source.setUrl(url);
    Flyway.configure().dataSource(source).target("7").load().migrate();
    var jdbc = new JdbcTemplate(source);
    long pending = System.currentTimeMillis()+60_000;
    for (int id=1; id<=3; id++) {
      String origin = id==3 ? "https://other.example" : "https://same.example";
      jdbc.update("""
          INSERT INTO browser_tasks(source_id,source_version,terms_version,origin,list_url,
            row_start,row_end,max_rows,max_chars,read_delay_seconds,adapter)
          VALUES (?,1,'approved',?,?,0,1,1,1000,1,'sub2api')
          """, id, origin, origin+"/table");
      jdbc.update("INSERT INTO browser_source_pacing VALUES (?,?)",id,pending+id*1000);
    }
    Flyway.configure().dataSource(source).load().migrate();
    var reopened = new SQLiteDataSource(); reopened.setUrl(url);
    var fresh = new JdbcTemplate(reopened);
    assertEquals(pending+2000,fresh.queryForObject(
        "SELECT last_read_at FROM origin_request_pacing WHERE origin='https://same.example'",Long.class));
    assertEquals(pending+3000,fresh.queryForObject(
        "SELECT last_read_at FROM origin_request_pacing WHERE origin='https://other.example'",Long.class));
    assertEquals(2,fresh.queryForObject("SELECT COUNT(*) FROM origin_request_pacing",Integer.class));
    assertEquals(0,fresh.queryForObject("SELECT COUNT(*) FROM sqlite_master WHERE name='browser_source_pacing'",Integer.class));
  }
}

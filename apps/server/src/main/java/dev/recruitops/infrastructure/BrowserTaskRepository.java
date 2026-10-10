package dev.recruitops.infrastructure;

import dev.recruitops.domain.BrowserTask;
import dev.recruitops.domain.Source;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BrowserTaskRepository {
  private final JdbcTemplate jdbc;
  public BrowserTaskRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public void approve(Source s, String adapter) {
    jdbc.update("""
        INSERT INTO browser_source_policies(source_id,source_version,terms_version,approved_at,adapter)
        VALUES (?,?,?,?,?) ON CONFLICT(source_id) DO UPDATE SET source_version=excluded.source_version,
        terms_version=excluded.terms_version,approved_at=excluded.approved_at,adapter=excluded.adapter,revoked=0
        """, s.id(),s.configVersion(),s.termsConfirmedAt(),Instant.now().toString(),adapter);
  }
  public String requirePolicy(Source s) {
    return jdbc.query("""
        SELECT adapter FROM browser_source_policies WHERE source_id=? AND source_version=?
        AND terms_version=? AND revoked=0
        """, (rs,row)->rs.getString(1),s.id(),s.configVersion(),s.termsConfirmedAt()).stream()
        .findFirst().orElseThrow(()->new IllegalStateException("Visible browser policy is not approved/current"));
  }
  public void revoke(long sourceId) { jdbc.update("UPDATE browser_source_policies SET revoked=1 WHERE source_id=?",sourceId); }
  @Transactional
  public BrowserTask create(Source s,int start,int end,int chars,String adapter,String origin,int readDelaySeconds,boolean fixtureMode) {
    jdbc.update("""
        INSERT INTO browser_tasks(source_id,source_version,terms_version,origin,list_url,row_start,row_end,
        max_rows,max_chars,adapter,read_delay_seconds,fixture_mode) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        """,s.id(),s.configVersion(),s.termsConfirmedAt(),origin,s.listUrl(),start,end,end-start,chars,adapter,readDelaySeconds,fixtureMode);
    return find(jdbc.queryForObject("SELECT last_insert_rowid()",Long.class));
  }
  public BrowserTask claim(long id,String worker) {
    String token=UUID.randomUUID().toString(); long now=System.currentTimeMillis();
    int changed=jdbc.update("""
        UPDATE browser_tasks SET status='CLAIMED',worker_id=?,lease_token=?,lease_expires_at=?
        WHERE id=? AND (status='PENDING' OR (status='CLAIMED' AND lease_expires_at<?))
        """,worker,token,now+120_000,id,now);
    if(changed!=1) throw new IllegalStateException("Task already claimed/completed");
    return find(id);
  }
  public boolean fence(long id,String worker,String token,String hash) {
    if(jdbc.update("""
        UPDATE browser_tasks SET status='COMPLETING' WHERE id=? AND status='CLAIMED' AND worker_id=?
        AND lease_token=? AND lease_expires_at>?
        """,id,worker,token,System.currentTimeMillis())==1) return true;
    boolean replay=jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM browser_tasks WHERE id=? AND status='COMPLETED' AND worker_id=?
        AND lease_token=? AND result_hash=?)
        """,Integer.class,id,worker,token,hash)==1;
    if(!replay) throw new IllegalStateException("Expired/stale task lease or changed replay");
    return false;
  }
  public long pace(long id,String worker,String token) {
    if(jdbc.update("UPDATE browser_tasks SET lease_expires_at=lease_expires_at WHERE id=? AND status='CLAIMED' AND worker_id=? AND lease_token=? AND lease_expires_at>?",id,worker,token,System.currentTimeMillis())!=1)
      throw new IllegalStateException("Expired/stale lease");
    BrowserTask task=find(id); long now=System.currentTimeMillis();
    jdbc.update("INSERT OR IGNORE INTO browser_source_pacing(source_id,next_read_at) VALUES (?,?)",task.sourceId(),now);
    long scheduled=Math.max(now,jdbc.queryForObject("SELECT next_read_at FROM browser_source_pacing WHERE source_id=?",Long.class,task.sourceId()));
    jdbc.update("UPDATE browser_source_pacing SET next_read_at=? WHERE source_id=?",scheduled+Math.max(100,task.minIntervalSeconds()*1000L),task.sourceId());
    return scheduled;
  }
  public void finish(long id,String hash) { jdbc.update("UPDATE browser_tasks SET status='COMPLETED',result_hash=? WHERE id=? AND status='COMPLETING'",hash,id); }
  public void stop(long id,String worker,String token,String reason) {
    if(jdbc.update("UPDATE browser_tasks SET status='BLOCKED',error=? WHERE id=? AND status='CLAIMED' AND worker_id=? AND lease_token=? AND lease_expires_at>?",reason,id,worker,token,System.currentTimeMillis())!=1)
      throw new IllegalStateException("Expired/stale lease");
  }
  public List<BrowserTask> list() { return jdbc.query(select()+" ORDER BY t.id DESC",this::map); }
  public BrowserTask find(long id) { return jdbc.query(select()+" WHERE t.id=?",this::map,id).stream().findFirst().orElseThrow(()->new MissingResourceException("Browser task not found")); }
  private String select() { return "SELECT t.*,s.selectors_json,s.min_interval_seconds,s.robots_url FROM browser_tasks t JOIN sources s ON s.id=t.source_id"; }
  private BrowserTask map(java.sql.ResultSet rs,int row) throws java.sql.SQLException {
    long expiry=rs.getLong("lease_expires_at"); Long nullableExpiry=rs.wasNull()?null:expiry;
    return new BrowserTask(rs.getLong("id"),rs.getLong("source_id"),rs.getInt("source_version"),rs.getString("terms_version"),rs.getString("origin"),rs.getString("list_url"),rs.getInt("row_start"),rs.getInt("row_end"),rs.getInt("max_rows"),rs.getInt("max_chars"),rs.getString("adapter"),rs.getString("status"),rs.getString("worker_id"),rs.getString("lease_token"),nullableExpiry,rs.getString("selectors_json"),rs.getInt("read_delay_seconds"),rs.getString("robots_url"),rs.getBoolean("fixture_mode"));
  }
}

package dev.recruitops.web;

import dev.recruitops.application.BrowserTaskService;
import dev.recruitops.domain.BrowserTask;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/browser-tasks")
public class BrowserTasksController {
  private final BrowserTaskService tasks;
  public BrowserTasksController(BrowserTaskService tasks) { this.tasks=tasks; }
  @GetMapping public Map<String,List<BrowserTask>> list() { return Map.of("items",tasks.list()); }
  @PostMapping public BrowserTask create(@RequestBody Create input) { return tasks.create(input.sourceId(),input.rowStart(),input.rowEnd(),input.maxChars()); }
  @PostMapping("/sources/{id}/approve") public Map<String,String> approve(@PathVariable long id,@RequestBody Approval input) {
    if(!input.visibleBrowserApproved()) throw new IllegalArgumentException("Explicit visible-browser approval required");
    tasks.approveSource(id,input.adapter());return Map.of("status","APPROVED");
  }
  @PostMapping("/sources/{id}/revoke") public Map<String,String> revoke(@PathVariable long id) { tasks.revokeSource(id);return Map.of("status","REVOKED"); }
  @PostMapping("/{id}/claim") public BrowserTask claim(@PathVariable long id,@RequestBody Lease input) { return tasks.claim(id,input.workerId()); }
  @PostMapping("/{id}/check-access") public Map<String,String> check(@PathVariable long id,@RequestBody Lease input) { tasks.checkAccess(id,input.workerId(),input.leaseToken());return Map.of("status","ALLOWED"); }
  @PostMapping("/{id}/check-request") public Map<String,String> requestCheck(@PathVariable long id,@RequestBody RequestCheck input) { tasks.checkRequest(id,input.workerId(),input.leaseToken(),input.requestUrl());return Map.of("status","ALLOWED"); }
  @PostMapping("/{id}/pace") public Map<String,Long> pace(@PathVariable long id,@RequestBody Lease input) { return Map.of("readAt",tasks.pace(id,input.workerId(),input.leaseToken())); }
  @PostMapping("/{id}/stop") public BrowserTask stop(@PathVariable long id,@RequestBody Lease input) { return tasks.stop(id,input.workerId(),input.leaseToken(),input.reason()); }
  @PostMapping("/{id}/complete") public BrowserTask complete(@PathVariable long id,@RequestBody BrowserTaskService.Completion input) { return tasks.complete(id,input); }
  public record Create(long sourceId,int rowStart,int rowEnd,int maxChars) {}
  public record Approval(boolean visibleBrowserApproved,String adapter) {}
  public record Lease(String workerId,String leaseToken,String reason) {}
  public record RequestCheck(String workerId,String leaseToken,String requestUrl) {}
}

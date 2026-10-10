package dev.recruitops.infrastructure;

import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisJobCache {
  private final StringRedisTemplate redis;
  private final boolean enabled;
  public RedisJobCache(StringRedisTemplate redis,
      @Value("${recruitops.redis.enabled:false}") boolean enabled) {
    this.redis = redis; this.enabled = enabled;
  }
  public Optional<String> get(String key) {
    if (!enabled) return Optional.empty();
    try { return Optional.ofNullable(redis.opsForValue().get(key)); }
    catch (RuntimeException unavailable) { return Optional.empty(); }
  }
  public void put(String key, String value) {
    if (!enabled) return;
    try { redis.opsForValue().set(key, value, Duration.ofSeconds(30)); }
    catch (RuntimeException unavailable) { /* SQL remains authoritative when the cache is unavailable. */ }
  }
}

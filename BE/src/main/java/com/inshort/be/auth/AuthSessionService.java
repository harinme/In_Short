package com.inshort.be.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {

  static final String SESSION_COOKIE_NAME = "IN_SHORT_SESSION";
  private static final String SESSION_KEY_PREFIX = "auth:session:";

  private final StringRedisTemplate redisTemplate;
  private final Duration sessionTimeout;

  public AuthSessionService(
      StringRedisTemplate redisTemplate,
      @Value("${app.auth.session-timeout:10m}") Duration sessionTimeout) {
    this.redisTemplate = redisTemplate;
    this.sessionTimeout = sessionTimeout;
  }

  public Session create(Long userId) {
    String sessionId = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(key(sessionId), userId.toString(), sessionTimeout);
    return new Session(sessionId, Instant.now().plus(sessionTimeout));
  }

  public Optional<Long> findUserId(String sessionId) {
    String userId = redisTemplate.opsForValue().get(key(sessionId));
    if (userId == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(Long.valueOf(userId));
    } catch (NumberFormatException exception) {
      delete(sessionId);
      return Optional.empty();
    }
  }

  public void delete(String sessionId) {
    redisTemplate.delete(key(sessionId));
  }

  public Optional<Instant> expiresAt(String sessionId) {
    Long remainingSeconds = redisTemplate.getExpire(key(sessionId), TimeUnit.SECONDS);
    if (remainingSeconds == null || remainingSeconds <= 0) {
      return Optional.empty();
    }
    return Optional.of(Instant.now().plusSeconds(remainingSeconds));
  }

  public record Session(String id, Instant expiresAt) {}

  private String key(String sessionId) {
    return SESSION_KEY_PREFIX + sessionId;
  }
}

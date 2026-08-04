package com.inshort.be.voice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VoiceConversationService {

  static final Duration CONVERSATION_TTL = Duration.ofMinutes(30);
  private static final String KEY_PREFIX = "voice:conversation:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public VoiceConversationService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  public VoiceConversation start() {
    UUID conversationId = UUID.randomUUID();
    Instant createdAt = Instant.now();
    String key = key(conversationId);

    redisTemplate.opsForList().rightPush(key, write(new Header(createdAt)));
    redisTemplate.expire(key, CONVERSATION_TTL);

    return new VoiceConversation(
        conversationId, createdAt, List.of(), CONVERSATION_TTL.toSeconds());
  }

  public VoiceConversation appendMessage(UUID conversationId, String content) {
    String key = requireConversation(conversationId);
    StoredMessage message = new StoredMessage(content, Instant.now());

    redisTemplate.opsForList().rightPush(key, write(message));
    redisTemplate.expire(key, CONVERSATION_TTL);

    return find(conversationId);
  }

  public VoiceConversation find(UUID conversationId) {
    String key = requireConversation(conversationId);
    List<String> values = redisTemplate.opsForList().range(key, 0, -1);
    if (values == null || values.isEmpty()) {
      throw notFound(conversationId);
    }

    Header header = read(values.getFirst(), Header.class);
    List<VoiceConversation.Message> messages =
        values.stream()
            .skip(1)
            .map(value -> read(value, StoredMessage.class))
            .map(message -> new VoiceConversation.Message(message.content(), message.createdAt()))
            .toList();

    Long ttlSeconds = redisTemplate.getExpire(key);
    return new VoiceConversation(
        conversationId,
        header.createdAt(),
        messages,
        Math.max(0, ttlSeconds == null ? 0 : ttlSeconds));
  }

  public void end(UUID conversationId) {
    String key = requireConversation(conversationId);
    redisTemplate.delete(key);
  }

  private String requireConversation(UUID conversationId) {
    String key = key(conversationId);
    if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
      throw notFound(conversationId);
    }
    return key;
  }

  private String key(UUID conversationId) {
    return KEY_PREFIX + conversationId;
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Redis value serialization failed", exception);
    }
  }

  private <T> T read(String value, Class<T> type) {
    try {
      return objectMapper.readValue(value, type);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Redis value deserialization failed", exception);
    }
  }

  private ResponseStatusException notFound(UUID conversationId) {
    return new ResponseStatusException(
        HttpStatus.NOT_FOUND, "Voice conversation not found: " + conversationId);
  }

  private record Header(Instant createdAt) {}

  private record StoredMessage(String content, Instant createdAt) {}
}

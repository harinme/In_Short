package com.inshort.be.voice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VoiceConversation(
    UUID conversationId, Instant createdAt, List<Message> messages, long ttlSeconds) {

  public record Message(String content, Instant createdAt) {}
}

package com.inshort.be.voice.stt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inshort.be.ai.VoiceInterpretationService;
import com.inshort.be.ai.dto.VoiceInterpretationResponse;
import com.inshort.be.voice.VoiceConversation;
import com.inshort.be.voice.VoiceConversationService;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VoiceTranscriptionService {

  private static final Duration PROCESSING_TTL = Duration.ofMinutes(1);
  private static final String PROCESSING = "PROCESSING";
  private static final String KEY_PREFIX = "voice:transcription:";
  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of(
          "audio/webm",
          "audio/wav",
          "audio/x-wav",
          "audio/mpeg",
          "audio/mp4",
          "audio/m4a",
          "audio/x-m4a",
          "audio/ogg",
          "audio/flac");

  private final VoiceConversationService conversationService;
  private final VoiceInterpretationService interpretationService;
  private final SpeechTranscriptionClient transcriptionClient;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final GroqSttProperties properties;

  public VoiceTranscriptionService(
      VoiceConversationService conversationService,
      VoiceInterpretationService interpretationService,
      SpeechTranscriptionClient transcriptionClient,
      StringRedisTemplate redisTemplate,
      ObjectMapper objectMapper,
      GroqSttProperties properties) {
    this.conversationService = conversationService;
    this.interpretationService = interpretationService;
    this.transcriptionClient = transcriptionClient;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  public TranscriptionResponse transcribe(
      UUID conversationId, UUID requestId, MultipartFile audio) {
    validate(audio);
    conversationService.find(conversationId);

    String key = key(conversationId, requestId);
    if (!Boolean.TRUE.equals(
        redisTemplate.opsForValue().setIfAbsent(key, PROCESSING, PROCESSING_TTL))) {
      return existingResult(key);
    }

    try {
      String transcript = transcriptionClient.transcribe(audio).trim();
      if (transcript.isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY, "No speech was recognized");
      }

      VoiceInterpretationResponse interpretation =
          interpretationService.interpret(conversationId, requestId, transcript);

      VoiceConversation conversation =
          conversationService.appendMessage(conversationId, transcript);
      VoiceConversation.Message storedMessage = conversation.messages().getLast();
      TranscriptionResponse response =
          new TranscriptionResponse(
              requestId, storedMessage.content(), storedMessage.createdAt(), interpretation);
      redisTemplate
          .opsForValue()
          .set(key, write(response), Duration.ofSeconds(conversation.ttlSeconds()));
      return response;
    } catch (RuntimeException exception) {
      redisTemplate.delete(key);
      throw exception;
    }
  }

  private void validate(MultipartFile audio) {
    if (audio == null || audio.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file is required");
    }
    if (audio.getSize() > properties.maxFileSize().toBytes()) {
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Audio file is too large");
    }

    String contentType = audio.getContentType();
    String normalized =
        contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    if (!SUPPORTED_CONTENT_TYPES.contains(normalized)) {
      throw new ResponseStatusException(
          HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported audio content type");
    }
  }

  private TranscriptionResponse existingResult(String key) {
    String value = redisTemplate.opsForValue().get(key);
    if (value == null || PROCESSING.equals(value)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "The transcription request is already being processed");
    }
    try {
      return objectMapper.readValue(value, TranscriptionResponse.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Transcription result deserialization failed", exception);
    }
  }

  private String write(TranscriptionResponse response) {
    try {
      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Transcription result serialization failed", exception);
    }
  }

  private String key(UUID conversationId, UUID requestId) {
    return KEY_PREFIX + conversationId + ":" + requestId;
  }
}

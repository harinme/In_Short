package com.inshort.be.voice.stt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inshort.be.voice.VoiceConversation;
import com.inshort.be.voice.VoiceConversationService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;

class VoiceTranscriptionServiceTests {

  private VoiceConversationService conversationService;
  private SpeechTranscriptionClient transcriptionClient;
  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private ObjectMapper objectMapper;
  private VoiceTranscriptionService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    conversationService = mock(VoiceConversationService.class);
    transcriptionClient = mock(SpeechTranscriptionClient.class);
    redisTemplate = mock(StringRedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    objectMapper = new ObjectMapper().findAndRegisterModules();
    GroqSttProperties properties =
        new GroqSttProperties(
            "test-key",
            "https://api.groq.com/openai/v1",
            "whisper-large-v3-turbo",
            "ko",
            DataSize.ofMegabytes(10));
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    service =
        new VoiceTranscriptionService(
            conversationService, transcriptionClient, redisTemplate, objectMapper, properties);
  }

  @Test
  void transcribesAndStoresOneMessage() {
    UUID conversationId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-08-10T01:00:00Z");
    MockMultipartFile audio =
        new MockMultipartFile("audio", "speech.webm", "audio/webm", new byte[] {1, 2, 3});
    VoiceConversation started = new VoiceConversation(conversationId, createdAt, List.of(), 1_800);
    VoiceConversation updated =
        new VoiceConversation(
            conversationId,
            createdAt,
            List.of(new VoiceConversation.Message("잔액을 알려줘", createdAt)),
            1_800);

    when(conversationService.find(conversationId)).thenReturn(started);
    when(valueOperations.setIfAbsent(
            "voice:transcription:" + conversationId + ":" + requestId,
            "PROCESSING",
            Duration.ofMinutes(1)))
        .thenReturn(true);
    when(transcriptionClient.transcribe(audio)).thenReturn("  잔액을 알려줘  ");
    when(conversationService.appendMessage(conversationId, "잔액을 알려줘")).thenReturn(updated);

    TranscriptionResponse response = service.transcribe(conversationId, requestId, audio);

    assertThat(response.transcript()).isEqualTo("잔액을 알려줘");
    assertThat(response.createdAt()).isEqualTo(createdAt);
    verify(conversationService).appendMessage(conversationId, "잔액을 알려줘");
  }

  @Test
  void returnsStoredResultForRetriedRequest() throws Exception {
    UUID conversationId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    MockMultipartFile audio =
        new MockMultipartFile("audio", "speech.webm", "audio/webm", new byte[] {1});
    TranscriptionResponse stored =
        new TranscriptionResponse(requestId, "잔액을 알려줘", Instant.parse("2026-08-10T01:00:00Z"));
    String key = "voice:transcription:" + conversationId + ":" + requestId;

    when(valueOperations.setIfAbsent(key, "PROCESSING", Duration.ofMinutes(1))).thenReturn(false);
    when(valueOperations.get(key)).thenReturn(objectMapper.writeValueAsString(stored));

    TranscriptionResponse response = service.transcribe(conversationId, requestId, audio);

    assertThat(response).isEqualTo(stored);
    verifyNoInteractions(transcriptionClient);
  }

  @Test
  void rejectsUnsupportedAudioBeforeCallingStt() {
    MockMultipartFile audio =
        new MockMultipartFile("audio", "speech.txt", "text/plain", new byte[] {1});

    assertThatThrownBy(() -> service.transcribe(UUID.randomUUID(), UUID.randomUUID(), audio))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("415 UNSUPPORTED_MEDIA_TYPE");
    verifyNoInteractions(transcriptionClient);
  }
}

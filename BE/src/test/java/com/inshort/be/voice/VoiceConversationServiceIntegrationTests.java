package com.inshort.be.voice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_INTEGRATION_TEST", matches = "true")
class VoiceConversationServiceIntegrationTests {

  @Autowired private VoiceConversationService conversationService;

  @Test
  void managesConversationLifecycleAndRefreshesTtl() throws InterruptedException {
    VoiceConversation started = conversationService.start();
    long initialTtl = started.ttlSeconds();

    Thread.sleep(1_100);
    VoiceConversation updated =
        conversationService.appendMessage(started.conversationId(), "안녕하세요");

    assertThat(updated.messages()).hasSize(1);
    assertThat(updated.messages().getFirst().content()).isEqualTo("안녕하세요");
    assertThat(updated.ttlSeconds()).isGreaterThanOrEqualTo(initialTtl - 1);

    conversationService.end(started.conversationId());

    assertThatThrownBy(() -> conversationService.find(started.conversationId()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404 NOT_FOUND");
  }

  @Test
  void rejectsUnknownConversation() {
    assertThatThrownBy(() -> conversationService.find(UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404 NOT_FOUND");
  }
}

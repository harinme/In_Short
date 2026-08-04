package com.inshort.be.voice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voice-conversations")
public class VoiceConversationController {

  private final VoiceConversationService conversationService;

  public VoiceConversationController(VoiceConversationService conversationService) {
    this.conversationService = conversationService;
  }

  @PostMapping
  public ResponseEntity<VoiceConversation> start() {
    VoiceConversation conversation = conversationService.start();
    return ResponseEntity.created(
            URI.create("/api/voice-conversations/" + conversation.conversationId()))
        .body(conversation);
  }

  @PostMapping("/{conversationId}/messages")
  public VoiceConversation appendMessage(
      @PathVariable UUID conversationId, @Valid @RequestBody MessageRequest request) {
    return conversationService.appendMessage(conversationId, request.content());
  }

  @GetMapping("/{conversationId}")
  public VoiceConversation find(@PathVariable UUID conversationId) {
    return conversationService.find(conversationId);
  }

  @DeleteMapping("/{conversationId}")
  public ResponseEntity<Void> end(@PathVariable UUID conversationId) {
    conversationService.end(conversationId);
    return ResponseEntity.noContent().build();
  }

  public record MessageRequest(@NotBlank @Size(max = 4_000) String content) {}
}

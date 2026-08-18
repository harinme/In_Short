package com.inshort.be.ai;

import com.inshort.be.conversation.enums.ConversationIntent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

  private final AiService aiService;
  private final VoiceIntentService voiceIntentService;

  public AiController(AiService aiService, VoiceIntentService voiceIntentService) {
    this.aiService = aiService;
    this.voiceIntentService = voiceIntentService;
  }

  @PostMapping("/test")
  public String test(@RequestBody String prompt) {
    return aiService.generate(prompt);
  }

  @PostMapping("/intent")
  public IntentResponse classifyIntent(@Valid @RequestBody IntentRequest request) {
    return new IntentResponse(voiceIntentService.classify(request.transcript()));
  }

  public record IntentRequest(@NotBlank String transcript) {}

  public record IntentResponse(ConversationIntent intent) {}
}

package com.inshort.be.ai;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

  private final AiService aiService;

  public AiController(AiService aiService) {
    this.aiService = aiService;
  }

  @PostMapping("/test")
  public String test(@RequestBody String prompt) {
    return aiService.generate(prompt);
  }
}

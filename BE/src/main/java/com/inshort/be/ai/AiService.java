package com.inshort.be.ai;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiService {

  private final OpenAIClient openAIClient;
  private final String model;

  public AiService(OpenAIClient openAIClient, @Value("${app.ai.model}") String model) {
    this.openAIClient = openAIClient;
    this.model = model;
  }

  public String generate(String prompt) {
    ResponseCreateParams params = ResponseCreateParams.builder().model(model).input(prompt).build();

    Response response = openAIClient.responses().create(params);

    return response.output().stream()
        .flatMap(item -> item.message().stream())
        .flatMap(message -> message.content().stream())
        .flatMap(content -> content.outputText().stream())
        .map(outputText -> outputText.text())
        .findFirst()
        .orElse("");
  }
}

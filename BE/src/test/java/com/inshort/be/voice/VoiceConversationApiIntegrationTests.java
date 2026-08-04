package com.inshort.be.voice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_INTEGRATION_TEST", matches = "true")
class VoiceConversationApiIntegrationTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void managesConversationThroughHttpApi() throws Exception {
    String startBody =
        mockMvc
            .perform(post("/api/voice-conversations"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ttlSeconds").value(1_800))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode started = objectMapper.readTree(startBody);
    String conversationId = started.get("conversationId").asText();

    mockMvc
        .perform(
            post("/api/voice-conversations/{conversationId}/messages", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"안녕하세요\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages[0].content").value("안녕하세요"));

    mockMvc
        .perform(get("/api/voice-conversations/{conversationId}", conversationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages.length()").value(1));

    mockMvc
        .perform(delete("/api/voice-conversations/{conversationId}", conversationId))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/voice-conversations/{conversationId}", conversationId))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsBlankMessage() throws Exception {
    String startBody =
        mockMvc
            .perform(post("/api/voice-conversations"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String conversationId = objectMapper.readTree(startBody).get("conversationId").asText();

    mockMvc
        .perform(
            post("/api/voice-conversations/{conversationId}/messages", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\" \"}"))
        .andExpect(status().isBadRequest());

    mockMvc.perform(delete("/api/voice-conversations/{conversationId}", conversationId));
  }
}

package com.inshort.be.voice;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inshort.be.ai.VoiceInterpretationService;
import com.inshort.be.ai.dto.VoiceInterpretationResponse;
import com.inshort.be.ai.dto.VoiceSlots;
import com.inshort.be.ai.dto.VoiceSlots.TransferSlots;
import com.inshort.be.ai.enums.InterpretationStatus;
import com.inshort.be.ai.enums.NextAction;
import com.inshort.be.conversation.enums.ConversationIntent;
import com.inshort.be.voice.stt.VoiceTranscriptionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VoiceConversationControllerTests {

  private final VoiceConversationService conversationService = mock(VoiceConversationService.class);
  private final VoiceTranscriptionService transcriptionService =
      mock(VoiceTranscriptionService.class);
  private final VoiceInterpretationService interpretationService =
      mock(VoiceInterpretationService.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    VoiceConversationController controller =
        new VoiceConversationController(
            conversationService, transcriptionService, interpretationService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void interpretsTranscriptForExistingConversation() throws Exception {
    UUID conversationId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    String transcript = "민수에게 삼만 원 보내줘";
    VoiceInterpretationResponse response =
        new VoiceInterpretationResponse(
            conversationId,
            requestId,
            transcript,
            ConversationIntent.TRANSFER,
            InterpretationStatus.READY,
            NextAction.OPEN_TRANSFER,
            new VoiceSlots(new TransferSlots("민수", 30_000L), null, null),
            List.of(),
            "민수님께 30000원 송금을 준비할게요.");

    when(conversationService.find(conversationId)).thenReturn(mock(VoiceConversation.class));
    when(interpretationService.interpret(conversationId, requestId, transcript))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/voice-conversations/{conversationId}/interpretations", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requestId": "%s",
                      "transcript": "%s"
                    }
                    """
                        .formatted(requestId, transcript)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
        .andExpect(jsonPath("$.requestId").value(requestId.toString()))
        .andExpect(jsonPath("$.intent").value("TRANSFER"))
        .andExpect(jsonPath("$.status").value("READY"))
        .andExpect(jsonPath("$.nextAction").value("OPEN_TRANSFER"))
        .andExpect(jsonPath("$.slots.transfer.recipientName").value("민수"))
        .andExpect(jsonPath("$.slots.transfer.amount").value(30_000));

    verify(conversationService).find(conversationId);
    verify(interpretationService).interpret(conversationId, requestId, transcript);
  }

  @Test
  void rejectsBlankTranscript() throws Exception {
    UUID conversationId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/voice-conversations/{conversationId}/interpretations", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requestId": "%s",
                      "transcript": " "
                    }
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest());
  }
}

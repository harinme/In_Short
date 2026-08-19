package com.inshort.be.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inshort.be.ai.dto.AiInterpretationResult;
import com.inshort.be.conversation.enums.ConversationIntent;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.StructuredResponseOutputItem;
import com.openai.models.responses.StructuredResponseOutputMessage;
import com.openai.models.responses.StructuredResponseOutputMessage.Content;
import com.openai.services.blocking.ResponseService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("unchecked")
class AiServiceTests {

  private final OpenAIClient openAIClient = mock(OpenAIClient.class);
  private final ResponseService responseService = mock(ResponseService.class);
  private final StructuredResponse<AiInterpretationResult> response =
      mock(StructuredResponse.class);
  private final AiService aiService = new AiService(openAIClient, "gpt-5.4-mini");

  @BeforeEach
  void setUp() {
    when(openAIClient.responses()).thenReturn(responseService);
    when(responseService.create(any(StructuredResponseCreateParams.class))).thenReturn(response);
  }

  @Test
  void returnsStructuredInterpretationFromOpenAiClient() {
    AiInterpretationResult expected =
        new AiInterpretationResult(ConversationIntent.TRANSFER, "민수", 30_000L, "", "", "");
    StructuredResponseOutputItem<AiInterpretationResult> outputItem =
        mock(StructuredResponseOutputItem.class);
    StructuredResponseOutputMessage<AiInterpretationResult> message =
        mock(StructuredResponseOutputMessage.class);
    Content<AiInterpretationResult> content = mock(Content.class);

    when(response.output()).thenReturn(List.of(outputItem));
    when(outputItem.message()).thenReturn(Optional.of(message));
    when(message.content()).thenReturn(List.of(content));
    when(content.outputText()).thenReturn(Optional.of(expected));

    AiInterpretationResult actual = aiService.interpret("민수에게 삼만 원 보내줘");

    assertThat(actual).isEqualTo(expected);
    ArgumentCaptor<StructuredResponseCreateParams<AiInterpretationResult>> paramsCaptor =
        ArgumentCaptor.forClass(StructuredResponseCreateParams.class);
    verify(responseService).create(paramsCaptor.capture());

    StructuredResponseCreateParams<AiInterpretationResult> params = paramsCaptor.getValue();
    assertThat(params.responseType()).isEqualTo(AiInterpretationResult.class);
    assertThat(params.rawParams().maxOutputTokens()).contains(500L);
    assertThat(params.rawParams().store()).contains(false);
    assertThat(params.rawParams().instructions()).isPresent();
  }

  @Test
  void returnsUnknownWhenOpenAiResponseHasNoOutput() {
    when(response.output()).thenReturn(List.of());

    AiInterpretationResult actual = aiService.interpret("분류할 수 없는 요청");

    assertThat(actual.intent()).isEqualTo(ConversationIntent.UNKNOWN);
    assertThat(actual.recipientName()).isEmpty();
    assertThat(actual.amount()).isZero();
  }
}

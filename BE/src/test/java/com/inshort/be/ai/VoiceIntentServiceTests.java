package com.inshort.be.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.inshort.be.conversation.enums.ConversationIntent;
import org.junit.jupiter.api.Test;

class VoiceIntentServiceTests {

  private final AiService aiService = mock(AiService.class);
  private final VoiceIntentService service = new VoiceIntentService(aiService);

  @Test
  void classifiesCommonFinancialRequestsWithoutAiCall() {
    assertThat(service.classify("김민수에게 돈을 보내줘")).isEqualTo(ConversationIntent.TRANSFER);
    assertThat(service.classify("내 계좌 잔액을 확인해 줘")).isEqualTo(ConversationIntent.BALANCE);
    assertThat(service.classify("최근 거래 내역을 보여줘")).isEqualTo(ConversationIntent.HISTORY);
  }

  @Test
  void usesAiForAnUnfamiliarExpression() {
    when(aiService.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("BALANCE");

    assertThat(service.classify("내가 가진 돈을 알고 싶어")).isEqualTo(ConversationIntent.BALANCE);
  }

  @Test
  void returnsUnknownForAnUnexpectedAiResponse() {
    when(aiService.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("설명할 수 없음");

    assertThat(service.classify("오늘 날씨가 어때")).isEqualTo(ConversationIntent.UNKNOWN);
  }
}

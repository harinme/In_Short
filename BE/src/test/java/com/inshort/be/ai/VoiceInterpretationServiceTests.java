package com.inshort.be.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.inshort.be.ai.dto.AiInterpretationResult;
import com.inshort.be.ai.dto.VoiceInterpretationResponse;
import com.inshort.be.ai.dto.VoiceSlots.BalanceSlots;
import com.inshort.be.ai.dto.VoiceSlots.TransferSlots;
import com.inshort.be.ai.enums.InterpretationStatus;
import com.inshort.be.ai.enums.MissingField;
import com.inshort.be.ai.enums.NextAction;
import com.inshort.be.conversation.enums.ConversationIntent;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VoiceInterpretationServiceTests {

  private final AiService aiService = mock(AiService.class);
  private final VoiceInterpretationService service = new VoiceInterpretationService(aiService);
  private final UUID conversationId = UUID.randomUUID();
  private final UUID requestId = UUID.randomUUID();

  @Test
  void preparesCompleteTransfer() {
    VoiceInterpretationResponse response =
        interpret("민수에게 삼만 원 보내줘", result(ConversationIntent.TRANSFER, "민수", 30_000L, "", "", ""));

    assertThat(response.status()).isEqualTo(InterpretationStatus.READY);
    assertThat(response.nextAction()).isEqualTo(NextAction.OPEN_TRANSFER);
    assertThat(response.missingFields()).isEmpty();
    assertThat(response.slots().transfer()).isEqualTo(new TransferSlots("민수", 30_000L));
  }

  @Test
  void asksForMissingAmount() {
    VoiceInterpretationResponse response =
        interpret("민수에게 보내줘", result(ConversationIntent.TRANSFER, "민수", 0, "", "", ""));

    assertThat(response.status()).isEqualTo(InterpretationStatus.NEEDS_CLARIFICATION);
    assertThat(response.nextAction()).isEqualTo(NextAction.ASK_FOLLOW_UP);
    assertThat(response.missingFields()).containsExactly(MissingField.AMOUNT);
    assertThat(response.message()).isEqualTo("민수님께 얼마를 보내실까요?");
  }

  @Test
  void asksForMissingRecipient() {
    VoiceInterpretationResponse response =
        interpret("삼만 원 보내줘", result(ConversationIntent.TRANSFER, " ", 30_000L, "", "", ""));

    assertThat(response.missingFields()).containsExactly(MissingField.RECIPIENT);
    assertThat(response.message()).isEqualTo("누구에게 보내실까요?");
  }

  @Test
  void asksForRecipientAndAmountWhenTransferSlotsAreMissing() {
    VoiceInterpretationResponse response =
        interpret("송금할래", result(ConversationIntent.TRANSFER, "", 0, "", "", ""));

    assertThat(response.missingFields())
        .containsExactly(MissingField.RECIPIENT, MissingField.AMOUNT);
    assertThat(response.message()).isEqualTo("누구에게 얼마를 보내실까요?");
  }

  @Test
  void treatsNonPositiveAmountAsMissing() {
    VoiceInterpretationResponse response =
        interpret("민수에게 0원 보내줘", result(ConversationIntent.TRANSFER, "민수", 0, "", "", ""));

    assertThat(response.missingFields()).containsExactly(MissingField.AMOUNT);
    assertThat(response.slots().transfer().amount()).isNull();
  }

  @Test
  void opensAccountsForBalanceRequest() {
    VoiceInterpretationResponse response =
        interpret("국민은행 잔액 알려줘", result(ConversationIntent.BALANCE, "", 0, "국민", "", ""));

    assertThat(response.status()).isEqualTo(InterpretationStatus.READY);
    assertThat(response.nextAction()).isEqualTo(NextAction.OPEN_ACCOUNTS);
    assertThat(response.slots().balance()).isEqualTo(new BalanceSlots("국민"));
  }

  @Test
  void opensHistoryForHistoryRequest() {
    VoiceInterpretationResponse response =
        interpret(
            "8월 거래 내역 보여줘",
            result(ConversationIntent.HISTORY, "", 0, "국민", "2026-08-01", "2026-08-19"));

    assertThat(response.status()).isEqualTo(InterpretationStatus.READY);
    assertThat(response.nextAction()).isEqualTo(NextAction.OPEN_HISTORY);
    assertThat(response.slots().history().fromDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(response.slots().history().toDate()).isEqualTo(LocalDate.of(2026, 8, 19));
  }

  @Test
  void ignoresInvalidHistoryDate() {
    VoiceInterpretationResponse response =
        interpret("거래 내역 보여줘", result(ConversationIntent.HISTORY, "", 0, "", "2026-99-99", ""));

    assertThat(response.slots().history().fromDate()).isNull();
    assertThat(response.slots().history().toDate()).isNull();
  }

  @Test
  void returnsUnsupportedForUnknownOrMissingAiResult() {
    VoiceInterpretationResponse unknown =
        interpret("오늘 날씨 어때", result(ConversationIntent.UNKNOWN, "", 0, "", "", ""));
    VoiceInterpretationResponse missing = interpret("알 수 없는 요청", null);

    assertThat(unknown.status()).isEqualTo(InterpretationStatus.UNSUPPORTED);
    assertThat(unknown.nextAction()).isEqualTo(NextAction.RETRY);
    assertThat(missing.intent()).isEqualTo(ConversationIntent.UNKNOWN);
  }

  private VoiceInterpretationResponse interpret(String transcript, AiInterpretationResult result) {
    when(aiService.interpret(transcript)).thenReturn(result);
    return service.interpret(conversationId, requestId, transcript);
  }

  private AiInterpretationResult result(
      ConversationIntent intent,
      String recipientName,
      long amount,
      String accountHint,
      String fromDate,
      String toDate) {
    return new AiInterpretationResult(intent, recipientName, amount, accountHint, fromDate, toDate);
  }
}

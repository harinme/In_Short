package com.inshort.be.ai;

import com.inshort.be.ai.dto.AiInterpretationResult;
import com.inshort.be.ai.dto.VoiceInterpretationResponse;
import com.inshort.be.ai.dto.VoiceSlots;
import com.inshort.be.ai.dto.VoiceSlots.BalanceSlots;
import com.inshort.be.ai.dto.VoiceSlots.HistorySlots;
import com.inshort.be.ai.dto.VoiceSlots.TransferSlots;
import com.inshort.be.ai.enums.InterpretationStatus;
import com.inshort.be.ai.enums.MissingField;
import com.inshort.be.ai.enums.NextAction;
import com.inshort.be.conversation.enums.ConversationIntent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class VoiceInterpretationService {

  private final AiService aiService;

  public VoiceInterpretationService(AiService aiService) {
    this.aiService = aiService;
  }

  public VoiceInterpretationResponse interpret(
      UUID conversationId, UUID requestId, String transcript) {
    AiInterpretationResult result = aiService.interpret(transcript);
    ConversationIntent intent =
        result == null || result.intent() == null ? ConversationIntent.UNKNOWN : result.intent();
    VoiceSlots slots = toVoiceSlots(result);

    return switch (intent) {
      case TRANSFER -> transferResponse(conversationId, requestId, transcript, slots);
      case BALANCE ->
          readyResponse(
              conversationId,
              requestId,
              transcript,
              intent,
              NextAction.OPEN_ACCOUNTS,
              slots,
              "계좌 잔액을 확인할게요.");
      case HISTORY ->
          readyResponse(
              conversationId,
              requestId,
              transcript,
              intent,
              NextAction.OPEN_HISTORY,
              slots,
              "거래 내역을 확인할게요.");
      case UNKNOWN -> unsupportedResponse(conversationId, requestId, transcript, slots);
    };
  }

  private VoiceSlots toVoiceSlots(AiInterpretationResult result) {
    if (result == null) {
      return new VoiceSlots(null, null, null);
    }

    TransferSlots transfer =
        new TransferSlots(normalize(result.recipientName()), positiveAmount(result.amount()));
    BalanceSlots balance = new BalanceSlots(normalize(result.accountHint()));
    HistorySlots history =
        new HistorySlots(
            normalize(result.accountHint()),
            parseDate(result.fromDate()),
            parseDate(result.toDate()));

    return switch (result.intent() == null ? ConversationIntent.UNKNOWN : result.intent()) {
      case TRANSFER -> new VoiceSlots(transfer, null, null);
      case BALANCE -> new VoiceSlots(null, balance, null);
      case HISTORY -> new VoiceSlots(null, null, history);
      case UNKNOWN -> new VoiceSlots(null, null, null);
    };
  }

  private VoiceInterpretationResponse transferResponse(
      UUID conversationId, UUID requestId, String transcript, VoiceSlots slots) {
    TransferSlots transfer = sanitizeTransfer(slots == null ? null : slots.transfer());
    List<MissingField> missingFields = new ArrayList<>();

    if (transfer.recipientName() == null) {
      missingFields.add(MissingField.RECIPIENT);
    }
    if (transfer.amount() == null) {
      missingFields.add(MissingField.AMOUNT);
    }

    VoiceSlots sanitizedSlots =
        new VoiceSlots(
            transfer,
            slots == null ? null : slots.balance(),
            slots == null ? null : slots.history());
    if (missingFields.isEmpty()) {
      return readyResponse(
          conversationId,
          requestId,
          transcript,
          ConversationIntent.TRANSFER,
          NextAction.OPEN_TRANSFER,
          sanitizedSlots,
          transfer.recipientName() + "님께 " + transfer.amount() + "원 송금을 준비할게요.");
    }

    return new VoiceInterpretationResponse(
        conversationId,
        requestId,
        transcript,
        ConversationIntent.TRANSFER,
        InterpretationStatus.NEEDS_CLARIFICATION,
        NextAction.ASK_FOLLOW_UP,
        sanitizedSlots,
        List.copyOf(missingFields),
        clarificationMessage(missingFields, transfer.recipientName()));
  }

  private VoiceInterpretationResponse readyResponse(
      UUID conversationId,
      UUID requestId,
      String transcript,
      ConversationIntent intent,
      NextAction nextAction,
      VoiceSlots slots,
      String message) {
    return new VoiceInterpretationResponse(
        conversationId,
        requestId,
        transcript,
        intent,
        InterpretationStatus.READY,
        nextAction,
        slots,
        List.of(),
        message);
  }

  private VoiceInterpretationResponse unsupportedResponse(
      UUID conversationId, UUID requestId, String transcript, VoiceSlots slots) {
    return new VoiceInterpretationResponse(
        conversationId,
        requestId,
        transcript,
        ConversationIntent.UNKNOWN,
        InterpretationStatus.UNSUPPORTED,
        NextAction.RETRY,
        slots,
        List.of(),
        "은행 업무와 관련된 요청을 다시 말씀해 주세요.");
  }

  private TransferSlots sanitizeTransfer(TransferSlots transfer) {
    if (transfer == null) {
      return new TransferSlots(null, null);
    }

    String recipientName =
        transfer.recipientName() == null || transfer.recipientName().isBlank()
            ? null
            : transfer.recipientName().trim();
    Long amount = transfer.amount() == null || transfer.amount() <= 0 ? null : transfer.amount();
    return new TransferSlots(recipientName, amount);
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private Long positiveAmount(long amount) {
    return amount > 0 ? amount : null;
  }

  private LocalDate parseDate(String value) {
    String normalized = normalize(value);
    if (normalized == null) {
      return null;
    }
    try {
      return LocalDate.parse(normalized);
    } catch (DateTimeParseException exception) {
      return null;
    }
  }

  private String clarificationMessage(List<MissingField> missingFields, String recipientName) {
    if (missingFields.contains(MissingField.RECIPIENT)
        && missingFields.contains(MissingField.AMOUNT)) {
      return "누구에게 얼마를 보내실까요?";
    }
    if (missingFields.contains(MissingField.RECIPIENT)) {
      return "누구에게 보내실까요?";
    }
    return recipientName + "님께 얼마를 보내실까요?";
  }
}

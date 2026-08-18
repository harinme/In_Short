package com.inshort.be.ai;

import com.inshort.be.conversation.enums.ConversationIntent;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class VoiceIntentService {

  private final AiService aiService;

  public VoiceIntentService(AiService aiService) {
    this.aiService = aiService;
  }

  public ConversationIntent classify(String transcript) {
    String normalized = transcript.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    if (containsAny(normalized, "송금", "이체", "보내줘", "보낼게", "돈보내")) {
      return ConversationIntent.TRANSFER;
    }
    if (containsAny(normalized, "거래내역", "사용내역", "입출금", "쓴내역")) {
      return ConversationIntent.HISTORY;
    }
    if (containsAny(normalized, "계좌", "잔액", "자산", "통장", "얼마있")) {
      return ConversationIntent.BALANCE;
    }

    String result =
        aiService.generate(
            "다음 금융 요청을 TRANSFER, BALANCE, HISTORY, UNKNOWN 중 하나로만 분류하세요. "
                + "설명은 쓰지 마세요. 요청: "
                + transcript);
    try {
      return ConversationIntent.valueOf(result.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return ConversationIntent.UNKNOWN;
    }
  }

  private boolean containsAny(String value, String... keywords) {
    for (String keyword : keywords) {
      if (value.contains(keyword)) return true;
    }
    return false;
  }
}

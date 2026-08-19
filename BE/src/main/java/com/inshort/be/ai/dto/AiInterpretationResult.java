package com.inshort.be.ai.dto;

import com.inshort.be.conversation.enums.ConversationIntent;

public record AiInterpretationResult(
    ConversationIntent intent,
    String recipientName,
    long amount,
    String accountHint,
    String fromDate,
    String toDate) {}

package com.inshort.be.ai.dto;

import com.inshort.be.ai.enums.InterpretationStatus;
import com.inshort.be.ai.enums.MissingField;
import com.inshort.be.ai.enums.NextAction;
import com.inshort.be.conversation.enums.ConversationIntent;
import java.util.List;
import java.util.UUID;

public record VoiceInterpretationResponse(
    UUID conversationId,
    UUID requestId,
    String transcript,
    ConversationIntent intent,
    InterpretationStatus status,
    NextAction nextAction,
    VoiceSlots slots,
    List<MissingField> missingFields,
    String message) {}

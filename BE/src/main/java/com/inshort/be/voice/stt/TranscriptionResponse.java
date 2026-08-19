package com.inshort.be.voice.stt;

import com.inshort.be.ai.dto.VoiceInterpretationResponse;
import java.time.Instant;
import java.util.UUID;

public record TranscriptionResponse(
    UUID requestId,
    String transcript,
    Instant createdAt,
    VoiceInterpretationResponse interpretation) {}

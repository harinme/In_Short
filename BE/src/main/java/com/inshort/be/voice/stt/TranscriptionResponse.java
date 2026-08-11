package com.inshort.be.voice.stt;

import java.time.Instant;
import java.util.UUID;

public record TranscriptionResponse(UUID requestId, String transcript, Instant createdAt) {}

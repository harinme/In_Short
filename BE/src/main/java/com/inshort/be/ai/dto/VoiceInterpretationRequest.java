package com.inshort.be.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VoiceInterpretationRequest(@NotNull UUID requestId, @NotBlank String transcript) {}

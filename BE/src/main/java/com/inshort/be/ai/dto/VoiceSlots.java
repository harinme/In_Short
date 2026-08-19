package com.inshort.be.ai.dto;

import java.time.LocalDate;

public record VoiceSlots(TransferSlots transfer, BalanceSlots balance, HistorySlots history) {

  public record TransferSlots(String recipientName, Long amount) {}

  public record BalanceSlots(String accountHint) {}

  public record HistorySlots(String accountHint, LocalDate fromDate, LocalDate toDate) {}
}

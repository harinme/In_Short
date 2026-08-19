package com.inshort.be.account.dto;

import java.time.LocalDate;
import java.util.List;

public record TransactionListResponse(
    String accountNumber,
    LocalDate from,
    LocalDate to,
    int page,
    int size,
    int numberOfElements,
    long totalElements,
    boolean hasNext,
    Integer nextPage,
    List<TransactionResponse> transactions) {

  public TransactionListResponse {
    transactions = List.copyOf(transactions);
  }
}

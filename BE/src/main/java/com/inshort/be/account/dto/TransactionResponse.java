package com.inshort.be.account.dto;

import com.inshort.be.transaction.entity.Transaction;
import com.inshort.be.transaction.enums.TransactionType;
import com.inshort.be.transaction.enums.TransactionStatus;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long transactionId,
    LocalDateTime transactedAt,
    TransactionType type,
    TransactionStatus status,
    long amount,
    long balanceAfter,
    String counterpartyBankName,
    String counterpartyBankCode,
    String counterpartyName,
    String counterpartyAccount,
    String memo) {

  public static TransactionResponse from(Transaction transaction) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getCreatedAt(),
        transaction.getTransactionType(),
        transaction.getStatus(),
        transaction.getAmount(),
        transaction.getBalanceAfter(),
        transaction.getCounterpartyBank().getName(),
        transaction.getCounterpartyBank().getCode(),
        transaction.getCounterpartyName(),
        transaction.getCounterpartyAccount(),
        transaction.getMemo());
  }
}

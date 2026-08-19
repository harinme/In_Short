package com.inshort.be.account.dto;

import com.inshort.be.transaction.entity.Transaction;
import com.inshort.be.transaction.enums.TransactionType;
import com.inshort.be.transaction.enums.TransactionChannel;
import com.inshort.be.transaction.enums.TransactionStatus;
import java.time.LocalDateTime;

public record TransactionDetailResponse(
    Long transactionId,
    String accountNumber,
    LocalDateTime transactedAt,
    TransactionType type,
    TransactionStatus status,
    long amount,
    long fee,
    long balanceAfter,
    TransactionChannel channel,
    String counterpartyBankName,
    String counterpartyBankCode,
    String counterpartyName,
    String counterpartyAccount,
    String memo,
    String transferId,
    String referenceNumber,
    LocalDateTime canceledAt,
    boolean receiptAvailable) {

  public static TransactionDetailResponse from(Transaction transaction) {
    return new TransactionDetailResponse(
        transaction.getId(),
        transaction.getAccount().getAccountNumber(),
        transaction.getCreatedAt(),
        transaction.getTransactionType(),
        transaction.getStatus(),
        transaction.getAmount(),
        transaction.getFee(),
        transaction.getBalanceAfter(),
        transaction.getChannel(),
        transaction.getCounterpartyBank().getName(),
        transaction.getCounterpartyBank().getCode(),
        transaction.getCounterpartyName(),
        transaction.getCounterpartyAccount(),
        transaction.getMemo(),
        transaction.getTransferId(),
        transaction.getReferenceNumber(),
        transaction.getCanceledAt(),
        transaction.getStatus() == TransactionStatus.COMPLETED
            && transaction.getTransferId() != null);
  }
}

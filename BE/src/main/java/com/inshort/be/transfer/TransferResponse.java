package com.inshort.be.transfer;

import java.util.List;

public record TransferResponse(
    String transferId,
    TransferResult result,
    long amount,
    long fee,
    Long balanceAfter,
    String recipientName,
    String recipientBankName,
    String recipientAccountNumber,
    RiskLevel riskLevel,
    List<RiskSignal> riskSignals) {}

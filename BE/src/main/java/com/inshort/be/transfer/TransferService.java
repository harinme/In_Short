package com.inshort.be.transfer;

import com.inshort.be.account.entity.Account;
import com.inshort.be.account.enums.AccountStatus;
import com.inshort.be.account.repository.AccountRepository;
import com.inshort.be.transaction.entity.Transaction;
import com.inshort.be.transaction.enums.TransactionStatus;
import com.inshort.be.transaction.enums.TransactionType;
import com.inshort.be.transaction.repository.TransactionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransferService {

  static final long LARGE_AMOUNT = 10_000_000L;
  static final long DAILY_REVIEW_AMOUNT = 50_000_000L;
  static final long DAILY_LIMIT = 500_000_000L;

  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final TransferRiskAssessmentRepository riskAssessmentRepository;

  public TransferService(
      AccountRepository accountRepository,
      TransactionRepository transactionRepository,
      TransferRiskAssessmentRepository riskAssessmentRepository) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.riskAssessmentRepository = riskAssessmentRepository;
  }

  @Transactional
  public TransferResponse transfer(Long userId, TransferRequest request) {
    Account ownedSource =
        accountRepository
            .findByUserIdAndAccountNumber(userId, request.sourceAccountNumber())
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found"));
    Account recipient =
        accountRepository
            .findByBankCodeAndAccountNumber(
                request.recipientBankCode(), request.recipientAccountNumber())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Recipient account not found"));

    LockedAccounts locked = lockAccounts(ownedSource, recipient);
    Account source = locked.source();
    recipient = locked.recipient();

    var existing =
        transactionRepository.findFirstByTransferIdAndTransactionType(
            request.requestId(), TransactionType.WITHDRAW);
    if (existing.isPresent()) {
      return completed(existing.get(), List.of(), RiskLevel.LOW);
    }
    validateActive(source, "Source");
    validateActive(recipient, "Recipient");
    if (source.getBalance() < request.amount()) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient balance");
    }

    long sentToday =
        transactionRepository.sumAmountSince(
            source.getId(),
            TransactionType.WITHDRAW,
            TransactionStatus.COMPLETED,
            LocalDate.now().atStartOfDay());
    if (sentToday + request.amount() > DAILY_LIMIT) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Daily transfer limit exceeded");
    }

    List<RiskSignal> signals = assess(source, recipient, request.amount(), sentToday);
    RiskLevel riskLevel = riskLevel(signals);
    if (riskLevel != RiskLevel.LOW && !request.riskConfirmed()) {
      riskAssessmentRepository.save(
          TransferRiskAssessment.of(
              source, recipient, request, riskLevel, signals, TransferResult.REVIEW_REQUIRED));
      return review(request, recipient, riskLevel, signals);
    }

    source.withdraw(request.amount());
    recipient.deposit(request.amount());
    String transferId = request.requestId().toLowerCase();
    Transaction withdrawal =
        ledger(
            source, recipient, request, transferId, TransactionType.WITHDRAW, source.getBalance());
    transactionRepository.save(withdrawal);
    transactionRepository.save(
        ledger(
            recipient,
            source,
            request,
            transferId,
            TransactionType.DEPOSIT,
            recipient.getBalance()));
    riskAssessmentRepository.save(
        TransferRiskAssessment.of(
            source, recipient, request, riskLevel, signals, TransferResult.COMPLETED));
    return completed(withdrawal, signals, riskLevel);
  }

  private LockedAccounts lockAccounts(Account source, Account recipient) {
    long firstId = Math.min(source.getId(), recipient.getId());
    long secondId = Math.max(source.getId(), recipient.getId());
    Account first = accountRepository.findByIdForUpdate(firstId).orElseThrow();
    Account second = accountRepository.findByIdForUpdate(secondId).orElseThrow();
    return source.getId().equals(firstId)
        ? new LockedAccounts(first, second)
        : new LockedAccounts(second, first);
  }

  private List<RiskSignal> assess(Account source, Account recipient, long amount, long sentToday) {
    List<RiskSignal> signals = new ArrayList<>();
    if (amount >= LARGE_AMOUNT) signals.add(RiskSignal.LARGE_AMOUNT);
    boolean knownRecipient =
        transactionRepository
            .existsByAccountIdAndCounterpartyBankIdAndCounterpartyAccountAndTransactionType(
                source.getId(),
                recipient.getBank().getId(),
                recipient.getAccountNumber(),
                TransactionType.WITHDRAW);
    if (!knownRecipient && amount >= 1_000_000L) signals.add(RiskSignal.NEW_RECIPIENT);
    if (sentToday + amount >= DAILY_REVIEW_AMOUNT) signals.add(RiskSignal.DAILY_ACCUMULATION);
    if (sentToday + amount >= DAILY_LIMIT * 9 / 10) signals.add(RiskSignal.NEAR_DAILY_LIMIT);
    return signals;
  }

  private RiskLevel riskLevel(List<RiskSignal> signals) {
    if (signals.contains(RiskSignal.NEAR_DAILY_LIMIT)
        || (signals.contains(RiskSignal.LARGE_AMOUNT)
            && signals.contains(RiskSignal.NEW_RECIPIENT))) {
      return RiskLevel.HIGH;
    }
    return signals.isEmpty() ? RiskLevel.LOW : RiskLevel.MEDIUM;
  }

  private void validateActive(Account account, String role) {
    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, role + " account is not active");
    }
  }

  private Transaction ledger(
      Account account,
      Account counterparty,
      TransferRequest request,
      String transferId,
      TransactionType type,
      long balanceAfter) {
    return Transaction.builder()
        .account(account)
        .counterpartyBank(counterparty.getBank())
        .transactionType(type)
        .status(TransactionStatus.COMPLETED)
        .amount(request.amount())
        .fee(0L)
        .channel(request.channel())
        .transferId(transferId)
        .referenceNumber(
            "HM-"
                + transferId.replace("-", "")
                + "-"
                + (type == TransactionType.WITHDRAW ? "W" : "D"))
        .balanceAfter(balanceAfter)
        .counterpartyName(counterparty.getHolder())
        .counterpartyAccount(counterparty.getAccountNumber())
        .memo(request.memo())
        .build();
  }

  private TransferResponse review(
      TransferRequest request, Account recipient, RiskLevel level, List<RiskSignal> signals) {
    return new TransferResponse(
        request.requestId(),
        TransferResult.REVIEW_REQUIRED,
        request.amount(),
        0,
        null,
        recipient.getHolder(),
        recipient.getBank().getName(),
        recipient.getAccountNumber(),
        level,
        signals);
  }

  private TransferResponse completed(
      Transaction transaction, List<RiskSignal> signals, RiskLevel level) {
    return new TransferResponse(
        transaction.getTransferId(),
        TransferResult.COMPLETED,
        transaction.getAmount(),
        transaction.getFee(),
        transaction.getBalanceAfter(),
        transaction.getCounterpartyName(),
        transaction.getCounterpartyBank().getName(),
        transaction.getCounterpartyAccount(),
        level,
        signals);
  }

  private record LockedAccounts(Account source, Account recipient) {}
}

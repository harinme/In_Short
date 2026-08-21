package com.inshort.be.transfer;

import com.inshort.be.account.entity.Account;
import com.inshort.be.account.enums.AccountStatus;
import com.inshort.be.account.repository.AccountRepository;
import com.inshort.be.contact.repository.ContactRepository;
import com.inshort.be.transaction.entity.Transaction;
import com.inshort.be.transaction.enums.TransactionChannel;
import com.inshort.be.transaction.enums.TransactionStatus;
import com.inshort.be.transaction.enums.TransactionType;
import com.inshort.be.transaction.repository.TransactionRepository;
import com.inshort.be.user.relationship.repository.UserRelationshipRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransferService {

  static final long LARGE_AMOUNT = 10_000_000L;
  static final long DAILY_REVIEW_AMOUNT = 50_000_000L;
  static final long DAILY_LIMIT = 500_000_000L;
  static final long SPLIT_TRANSFER_AMOUNT = 10_000_000L;

  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final TransferRiskAssessmentRepository riskAssessmentRepository;
  private final ContactRepository contactRepository;
  private final UserRelationshipRepository relationshipRepository;
  private final PasswordEncoder passwordEncoder;

  public TransferService(
      AccountRepository accountRepository,
      TransactionRepository transactionRepository,
      TransferRiskAssessmentRepository riskAssessmentRepository,
      ContactRepository contactRepository,
      UserRelationshipRepository relationshipRepository,
      PasswordEncoder passwordEncoder) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.riskAssessmentRepository = riskAssessmentRepository;
    this.contactRepository = contactRepository;
    this.relationshipRepository = relationshipRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public RecipientResponse findRecipient(Long userId, String bankCode, String accountNumber) {
    Account recipient =
        accountRepository
            .findByBankCodeAndAccountNumber(bankCode, accountNumber)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Recipient account not found"));
    validateActive(recipient, "Recipient");
    return RecipientResponse.from(recipient, userId);
  }

  @Transactional(readOnly = true)
  public List<RecipientSuggestionResponse> findRecipientSuggestions(Long userId) {
    var suggestions = new LinkedHashMap<String, RecipientSuggestionResponse>();
    contactRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
        .sorted((left, right) -> Boolean.compare(right.getFavorite(), left.getFavorite()))
        .map(
            contact ->
                new RecipientSuggestionResponse(
                    contact.getBank().getCode(),
                    contact.getBank().getName(),
                    contact.getAccountNumber(),
                    contact.getRecipientName(),
                    contact.getAlias(),
                    contact.getFavorite(),
                    true,
                    findRelationshipType(
                        userId, contact.getBank().getCode(), contact.getAccountNumber())))
        .forEach(
            item -> suggestions.put(suggestionKey(item.bankCode(), item.accountNumber()), item));

    transactionRepository
        .findRecentCounterparties(
            userId,
            TransactionType.WITHDRAW,
            TransactionStatus.COMPLETED,
            TransactionChannel.AUTO_TRANSFER,
            PageRequest.of(0, 30))
        .stream()
        .map(
            transaction ->
                new RecipientSuggestionResponse(
                    transaction.getCounterpartyBank().getCode(),
                    transaction.getCounterpartyBank().getName(),
                    transaction.getCounterpartyAccount(),
                    transaction.getCounterpartyName(),
                    null,
                    false,
                    false,
                    findRelationshipType(
                        userId,
                        transaction.getCounterpartyBank().getCode(),
                        transaction.getCounterpartyAccount())))
        .forEach(
            item ->
                suggestions.putIfAbsent(
                    suggestionKey(item.bankCode(), item.accountNumber()), item));
    return suggestions.values().stream().limit(10).toList();
  }

  private String suggestionKey(String bankCode, String accountNumber) {
    return bankCode + ':' + accountNumber.replace("-", "");
  }

  private com.inshort.be.user.relationship.enums.RelationshipType findRelationshipType(
      Long userId, String bankCode, String accountNumber) {
    return accountRepository
        .findByBankCodeAndAccountNumber(bankCode, accountNumber)
        .filter(account -> !account.getUser().getId().equals(userId))
        .flatMap(
            account ->
                relationshipRepository.findByUserIdAndRelatedUserId(
                    userId, account.getUser().getId()))
        .map(com.inshort.be.user.relationship.entity.UserRelationship::getRelationshipType)
        .orElse(null);
  }

  @Transactional(noRollbackFor = InvalidConfirmationPinException.class)
  public TransferResponse transfer(Long userId, TransferRequest request) {
    TransferRiskAssessment priorReview = null;
    Account ownedSource;
    if (request.riskConfirmed()) {
      ReviewValidation validation = validatePriorReviewBeforeAccountLock(userId, request);
      if (validation.blocked()) {
        Account blockedRecipient = findRecipient(request);
        return blocked(
            request, blockedRecipient, RiskLevel.HIGH, List.of(RiskSignal.PIN_CONFIRMATION_FAILED));
      }
      priorReview = validation.review();
      ownedSource = priorReview.getSourceAccount();
    } else {
      ownedSource =
          accountRepository
              .findByUserIdAndAccountNumber(userId, request.sourceAccountNumber())
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.NOT_FOUND, "Source account not found"));
    }
    Account recipient = findRecipient(request);

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

    List<RiskSignal> signals =
        priorReview == null
            ? assess(userId, source, recipient, request.amount(), sentToday)
            : signalsOf(priorReview);
    RiskLevel riskLevel = priorReview == null ? riskLevel(signals) : priorReview.getRiskLevel();
    if (priorReview != null
        && riskAssessmentRepository.existsRecentBlockedRecipient(
            userId,
            recipient.getBank().getCode(),
            recipient.getAccountNumber(),
            TransferResult.BLOCKED,
            LocalDateTime.now().minusMinutes(30))) {
      return blocked(
          request, recipient, RiskLevel.HIGH, List.of(RiskSignal.RECENT_HIGH_RISK_RECIPIENT));
    }
    if (riskLevel == RiskLevel.HIGH) {
      riskAssessmentRepository.save(
          TransferRiskAssessment.of(
              source, recipient, request, riskLevel, signals, TransferResult.BLOCKED));
      return blocked(request, recipient, riskLevel, signals);
    }
    if (riskLevel == RiskLevel.MEDIUM && !request.riskConfirmed()) {
      riskAssessmentRepository.save(
          TransferRiskAssessment.of(
              source, recipient, request, riskLevel, signals, TransferResult.REVIEW_REQUIRED));
      return review(request, recipient, riskLevel, signals);
    }
    if (priorReview != null) {
      priorReview.confirmAndConsume(LocalDateTime.now());
      riskAssessmentRepository.save(priorReview);
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

  private List<RiskSignal> assess(
      Long userId, Account source, Account recipient, long amount, long sentToday) {
    List<RiskSignal> signals = new ArrayList<>();
    if (riskAssessmentRepository.existsRecentBlockedRecipient(
        userId,
        recipient.getBank().getCode(),
        recipient.getAccountNumber(),
        TransferResult.BLOCKED,
        LocalDateTime.now().minusMinutes(30))) {
      signals.add(RiskSignal.RECENT_HIGH_RISK_RECIPIENT);
    }
    if (amount >= LARGE_AMOUNT) signals.add(RiskSignal.LARGE_AMOUNT);
    long recentCompletedTransfers =
        transactionRepository.countTransfersToRecipientSince(
            source.getId(),
            recipient.getBank().getId(),
            recipient.getAccountNumber(),
            TransactionType.WITHDRAW,
            TransactionStatus.COMPLETED,
            LocalDateTime.now().minusDays(90));
    boolean knownRecipient = recentCompletedTransfers >= 2;
    if (!knownRecipient && amount >= 1_000_000L) signals.add(RiskSignal.NEW_RECIPIENT);
    if (sentToday + amount >= DAILY_REVIEW_AMOUNT) signals.add(RiskSignal.DAILY_ACCUMULATION);
    if (sentToday + amount >= DAILY_LIMIT * 9 / 10) signals.add(RiskSignal.NEAR_DAILY_LIMIT);
    long recentTransferCount =
        transactionRepository.countTransfersSince(
            source.getId(),
            TransactionType.WITHDRAW,
            TransactionStatus.COMPLETED,
            LocalDateTime.now().minusMinutes(10));
    if (recentTransferCount + 1 >= 3) signals.add(RiskSignal.RAPID_TRANSFERS);
    LocalDateTime splitWindowStart = LocalDateTime.now().minusMinutes(30);
    long recentRecipientTransferCount =
        transactionRepository.countTransfersToRecipientSince(
            source.getId(),
            recipient.getBank().getId(),
            recipient.getAccountNumber(),
            TransactionType.WITHDRAW,
            TransactionStatus.COMPLETED,
            splitWindowStart);
    long recentRecipientAmount =
        transactionRepository.sumTransfersToRecipientSince(
            source.getId(),
            recipient.getBank().getId(),
            recipient.getAccountNumber(),
            TransactionType.WITHDRAW,
            TransactionStatus.COMPLETED,
            splitWindowStart);
    if (recentRecipientTransferCount + 1 >= 2
        && recentRecipientAmount + amount >= SPLIT_TRANSFER_AMOUNT) {
      signals.add(RiskSignal.SPLIT_TRANSFER);
    }
    return signals;
  }

  private RiskLevel riskLevel(List<RiskSignal> signals) {
    if (signals.contains(RiskSignal.NEAR_DAILY_LIMIT)
        || signals.contains(RiskSignal.SPLIT_TRANSFER)
        || signals.contains(RiskSignal.RECENT_HIGH_RISK_RECIPIENT)
        || signals.contains(RiskSignal.PIN_CONFIRMATION_FAILED)
        || (signals.contains(RiskSignal.LARGE_AMOUNT)
            && signals.contains(RiskSignal.NEW_RECIPIENT))) {
      return RiskLevel.HIGH;
    }
    return signals.isEmpty() ? RiskLevel.LOW : RiskLevel.MEDIUM;
  }

  private ReviewValidation validatePriorReviewBeforeAccountLock(
      Long userId, TransferRequest request) {
    TransferRiskAssessment review =
        riskAssessmentRepository
            .findFirstByTransferIdAndResultOrderByIdDesc(
                request.requestId().toLowerCase(), TransferResult.REVIEW_REQUIRED)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "Risk review is required before confirmation"));

    boolean sameTransfer =
        review.getSourceAccount().getUser().getId().equals(userId)
            && review.getSourceAccount().getAccountNumber().equals(request.sourceAccountNumber())
            && review.getRecipientBankCode().equals(request.recipientBankCode())
            && review.getRecipientAccountNumber().equals(request.recipientAccountNumber())
            && review.getAmount().equals(request.amount())
            && review.getRiskLevel() == RiskLevel.MEDIUM;
    if (!sameTransfer) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Transfer details changed after risk review");
    }
    LocalDateTime now = LocalDateTime.now();
    if (review.isExpired(now)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Risk review has expired");
    }
    if (review.isConsumed()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Risk review was already used");
    }
    if (request.confirmationPin() == null
        || !passwordEncoder.matches(
            request.confirmationPin(), review.getSourceAccount().getUser().getPinHash())) {
      boolean blocked = review.registerFailedConfirmation(LocalDateTime.now());
      riskAssessmentRepository.save(review);
      if (blocked) return new ReviewValidation(review, true);
      throw new InvalidConfirmationPinException();
    }
    return new ReviewValidation(review, false);
  }

  private Account findRecipient(TransferRequest request) {
    return accountRepository
        .findByBankCodeAndAccountNumber(
            request.recipientBankCode(), request.recipientAccountNumber())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient account not found"));
  }

  private List<RiskSignal> signalsOf(TransferRiskAssessment review) {
    if (review.getRiskSignals().isBlank()) return List.of();
    return java.util.Arrays.stream(review.getRiskSignals().split(","))
        .map(RiskSignal::valueOf)
        .toList();
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
        signals,
        RequiredAction.CUSTOMER_CONFIRMATION,
        reviewQuestion(signals),
        null);
  }

  private TransferResponse blocked(
      TransferRequest request, Account recipient, RiskLevel level, List<RiskSignal> signals) {
    return new TransferResponse(
        request.requestId(),
        TransferResult.BLOCKED,
        request.amount(),
        0,
        null,
        recipient.getHolder(),
        recipient.getBank().getName(),
        recipient.getAccountNumber(),
        level,
        signals,
        RequiredAction.STOP_AND_VERIFY,
        ReviewQuestion.NONE,
        caseReference(request.requestId()));
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
        signals,
        RequiredAction.NONE,
        ReviewQuestion.NONE,
        null);
  }

  private String caseReference(String transferId) {
    String value = transferId.replace("-", "").substring(0, 12).toUpperCase();
    return "HM-" + value.substring(0, 4) + "-" + value.substring(4, 8) + "-" + value.substring(8);
  }

  private ReviewQuestion reviewQuestion(List<RiskSignal> signals) {
    if (signals.contains(RiskSignal.RAPID_TRANSFERS)) {
      return ReviewQuestion.REPEATED_TRANSFER_INSTRUCTION;
    }
    if (signals.contains(RiskSignal.DAILY_ACCUMULATION)) {
      return ReviewQuestion.SAFE_ACCOUNT_INSTRUCTION;
    }
    return ReviewQuestion.REQUESTED_BY_OTHER;
  }

  private record LockedAccounts(Account source, Account recipient) {}

  private record ReviewValidation(TransferRiskAssessment review, boolean blocked) {}
}

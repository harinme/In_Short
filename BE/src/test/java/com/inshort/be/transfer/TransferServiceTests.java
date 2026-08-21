package com.inshort.be.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inshort.be.account.entity.Account;
import com.inshort.be.account.enums.AccountStatus;
import com.inshort.be.account.repository.AccountRepository;
import com.inshort.be.bank.entity.Bank;
import com.inshort.be.contact.repository.ContactRepository;
import com.inshort.be.transaction.entity.Transaction;
import com.inshort.be.transaction.enums.TransactionChannel;
import com.inshort.be.transaction.enums.TransactionStatus;
import com.inshort.be.transaction.enums.TransactionType;
import com.inshort.be.transaction.repository.TransactionRepository;
import com.inshort.be.user.entity.User;
import com.inshort.be.user.relationship.repository.UserRelationshipRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TransferServiceTests {

  @Mock private AccountRepository accountRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private TransferRiskAssessmentRepository riskAssessmentRepository;
  @Mock private ContactRepository contactRepository;
  @Mock private UserRelationshipRepository relationshipRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private TransferService transferService;
  private Account source;
  private Account recipient;

  @BeforeEach
  void setUp() {
    Bank sourceBank = Bank.builder().id(1L).code("100").name("한마디은행").build();
    Bank recipientBank = Bank.builder().id(2L).code("200").name("다온은행").build();
    source = account(10L, 1L, sourceBank, "100-01-000001", "김한마디", 20_000_000L);
    recipient = account(20L, 2L, recipientBank, "200-01-000001", "박다온", 1_000_000L);
    transferService =
        new TransferService(
            accountRepository,
            transactionRepository,
            riskAssessmentRepository,
            contactRepository,
            relationshipRepository,
            passwordEncoder);
  }

  @Test
  void verifiesRecipientFromRegisteredBankAccount() {
    when(accountRepository.findByBankCodeAndAccountNumber("200", "20001000001"))
        .thenReturn(Optional.of(recipient));

    RecipientResponse response = transferService.findRecipient(1L, "200", "20001000001");

    assertThat(response.bankName()).isEqualTo("다온은행");
    assertThat(response.accountNumber()).isEqualTo("200-01-000001");
    assertThat(response.holder()).isEqualTo("박다온");
  }

  @Test
  void transfersBetweenRegisteredBanksAndWritesTwoLedgerRows() {
    mockAccounts();
    when(transactionRepository.sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class)))
        .thenReturn(0L);
    mockKnownRecipient();

    TransferResponse response = transferService.transfer(1L, request(500_000L, false));

    assertThat(response.result()).isEqualTo(TransferResult.COMPLETED);
    assertThat(source.getBalance()).isEqualTo(19_500_000L);
    assertThat(recipient.getBalance()).isEqualTo(1_500_000L);
    ArgumentCaptor<Transaction> ledger = ArgumentCaptor.forClass(Transaction.class);
    verify(transactionRepository, org.mockito.Mockito.times(2)).save(ledger.capture());
    assertThat(ledger.getAllValues())
        .extracting(Transaction::getTransactionType)
        .containsExactly(TransactionType.WITHDRAW, TransactionType.DEPOSIT);
  }

  @Test
  void blocksHighRiskTransferToLargeNewRecipientWithoutChangingBalances() {
    mockAccounts();
    when(transactionRepository.sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class)))
        .thenReturn(0L);

    TransferResponse response = transferService.transfer(1L, request(10_000_000L, false));

    assertThat(response.result()).isEqualTo(TransferResult.BLOCKED);
    assertThat(response.riskLevel()).isEqualTo(RiskLevel.HIGH);
    assertThat(response.requiredAction()).isEqualTo(RequiredAction.STOP_AND_VERIFY);
    assertThat(response.caseReference()).isEqualTo("HM-1111-1111-1111");
    assertThat(response.riskSignals())
        .containsExactly(RiskSignal.LARGE_AMOUNT, RiskSignal.NEW_RECIPIENT);
    assertThat(source.getBalance()).isEqualTo(20_000_000L);
    verify(riskAssessmentRepository).save(any());
  }

  @Test
  void requiresCustomerConfirmationForMediumRiskTransfer() {
    mockAccounts();
    when(transactionRepository.sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class)))
        .thenReturn(0L);
    mockKnownRecipient();

    TransferResponse response = transferService.transfer(1L, request(10_000_000L, false));

    assertThat(response.result()).isEqualTo(TransferResult.REVIEW_REQUIRED);
    assertThat(response.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
    assertThat(response.riskSignals()).containsExactly(RiskSignal.LARGE_AMOUNT);
    assertThat(response.requiredAction()).isEqualTo(RequiredAction.CUSTOMER_CONFIRMATION);
    assertThat(response.reviewQuestion()).isEqualTo(ReviewQuestion.REQUESTED_BY_OTHER);
    assertThat(source.getBalance()).isEqualTo(20_000_000L);
    assertThat(recipient.getBalance()).isEqualTo(1_000_000L);
  }

  @Test
  void treatsRecipientWithOnlyOneRecentTransferAsNew() {
    mockAccounts();
    when(transactionRepository.sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class)))
        .thenReturn(0L);
    when(transactionRepository.countTransfersToRecipientSince(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(2L),
            org.mockito.ArgumentMatchers.eq("200-01-000001"),
            org.mockito.ArgumentMatchers.eq(TransactionType.WITHDRAW),
            org.mockito.ArgumentMatchers.eq(TransactionStatus.COMPLETED),
            any(LocalDateTime.class)))
        .thenReturn(1L);

    TransferResponse response = transferService.transfer(1L, request(1_000_000L, false));

    assertThat(response.result()).isEqualTo(TransferResult.REVIEW_REQUIRED);
    assertThat(response.riskSignals()).containsExactly(RiskSignal.NEW_RECIPIENT);
  }

  @Test
  void requiresReviewOnThirdTransferWithinTenMinutes() {
    mockAccounts();
    when(transactionRepository.sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class)))
        .thenReturn(0L);
    mockKnownRecipient();
    when(transactionRepository.countTransfersSince(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(TransactionType.WITHDRAW),
            org.mockito.ArgumentMatchers.eq(TransactionStatus.COMPLETED),
            any(LocalDateTime.class)))
        .thenReturn(2L);

    TransferResponse response = transferService.transfer(1L, request(500_000L, false));

    assertThat(response.result()).isEqualTo(TransferResult.REVIEW_REQUIRED);
    assertThat(response.riskSignals()).containsExactly(RiskSignal.RAPID_TRANSFERS);
    assertThat(response.reviewQuestion()).isEqualTo(ReviewQuestion.REPEATED_TRANSFER_INSTRUCTION);
  }

  @Test
  void blocksSecondSplitTransferWhenThirtyMinuteTotalReachesTenMillion() {
    mockAccounts();
    when(transactionRepository.sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class)))
        .thenReturn(4_000_000L);
    when(transactionRepository.countTransfersToRecipientSince(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(2L),
            org.mockito.ArgumentMatchers.eq("200-01-000001"),
            org.mockito.ArgumentMatchers.eq(TransactionType.WITHDRAW),
            org.mockito.ArgumentMatchers.eq(TransactionStatus.COMPLETED),
            any(LocalDateTime.class)))
        .thenReturn(0L, 1L);
    when(transactionRepository.sumTransfersToRecipientSince(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(2L),
            org.mockito.ArgumentMatchers.eq("200-01-000001"),
            org.mockito.ArgumentMatchers.eq(TransactionType.WITHDRAW),
            org.mockito.ArgumentMatchers.eq(TransactionStatus.COMPLETED),
            any(LocalDateTime.class)))
        .thenReturn(4_000_000L);

    TransferResponse response = transferService.transfer(1L, request(6_000_000L, false));

    assertThat(response.result()).isEqualTo(TransferResult.BLOCKED);
    assertThat(response.riskSignals())
        .containsExactly(RiskSignal.NEW_RECIPIENT, RiskSignal.SPLIT_TRANSFER);
    assertThat(source.getBalance()).isEqualTo(20_000_000L);
  }

  @Test
  void blocksLowerAmountRetryToRecipientBlockedWithinThirtyMinutes() {
    mockAccounts();
    when(transactionRepository.sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class)))
        .thenReturn(0L);
    mockKnownRecipient();
    when(riskAssessmentRepository.existsRecentBlockedRecipient(
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.eq("200"),
            org.mockito.ArgumentMatchers.eq("200-01-000001"),
            org.mockito.ArgumentMatchers.eq(TransferResult.BLOCKED),
            any(LocalDateTime.class)))
        .thenReturn(true);

    TransferResponse response = transferService.transfer(1L, request(500_000L, false));

    assertThat(response.result()).isEqualTo(TransferResult.BLOCKED);
    assertThat(response.riskSignals()).containsExactly(RiskSignal.RECENT_HIGH_RISK_RECIPIENT);
    assertThat(source.getBalance()).isEqualTo(20_000_000L);
    assertThat(recipient.getBalance()).isEqualTo(1_000_000L);
  }

  @Test
  void rejectsClientConfirmationWithoutServerReviewBeforeRiskCalculation() {
    assertThatThrownBy(() -> transferService.transfer(1L, request(10_000_000L, true)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Risk review is required");
    assertThat(source.getBalance()).isEqualTo(20_000_000L);
    assertThat(recipient.getBalance()).isEqualTo(1_000_000L);
  }

  @Test
  void rejectsRiskConfirmationWithoutPriorServerReview() {
    when(riskAssessmentRepository.findFirstByTransferIdAndResultOrderByIdDesc(
            "11111111-1111-1111-1111-111111111111", TransferResult.REVIEW_REQUIRED))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> transferService.transfer(1L, request(10_000_000L, true)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Risk review is required");
    assertThat(source.getBalance()).isEqualTo(20_000_000L);
    assertThat(recipient.getBalance()).isEqualTo(1_000_000L);
  }

  @Test
  void completesMediumRiskTransferOnlyWhenPriorReviewMatches() {
    mockConfirmedAccounts();
    when(transactionRepository.sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class)))
        .thenReturn(0L);
    TransferRiskAssessment priorReview =
        TransferRiskAssessment.of(
            source,
            recipient,
            request(10_000_000L, false),
            RiskLevel.MEDIUM,
            java.util.List.of(RiskSignal.LARGE_AMOUNT),
            TransferResult.REVIEW_REQUIRED);
    when(riskAssessmentRepository.findFirstByTransferIdAndResultOrderByIdDesc(
            "11111111-1111-1111-1111-111111111111", TransferResult.REVIEW_REQUIRED))
        .thenReturn(Optional.of(priorReview));
    when(passwordEncoder.matches("111111", "hash")).thenReturn(true);

    TransferResponse response = transferService.transfer(1L, request(10_000_000L, true));

    assertThat(response.result()).isEqualTo(TransferResult.COMPLETED);
    assertThat(response.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
    assertThat(response.requiredAction()).isEqualTo(RequiredAction.NONE);
    assertThat(source.getBalance()).isEqualTo(10_000_000L);
    assertThat(recipient.getBalance()).isEqualTo(11_000_000L);
  }

  @Test
  void rejectsMediumRiskTransferWhenConfirmationPinIsInvalid() {
    TransferRiskAssessment review = mediumReview(LocalDateTime.now().plusMinutes(5));
    mockMediumRiskConfirmation(review);
    when(passwordEncoder.matches("111111", "hash")).thenReturn(false);

    assertThatThrownBy(() -> transferService.transfer(1L, request(10_000_000L, true)))
        .isInstanceOf(InvalidConfirmationPinException.class)
        .hasMessageContaining("Invalid confirmation PIN");
    assertThat(review.getConfirmationAttempts()).isEqualTo(1);
    verify(riskAssessmentRepository).save(review);
    verify(accountRepository, never()).findByIdForUpdate(any());
    verify(accountRepository, never()).findByBankCodeAndAccountNumber(any(), any());
    verify(transactionRepository, never())
        .sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class));
    assertThat(source.getBalance()).isEqualTo(20_000_000L);
  }

  @Test
  void blocksRecipientWhenFifthConfirmationPinAttemptFails() {
    TransferRiskAssessment review = mediumReview(LocalDateTime.now().plusMinutes(5));
    for (int attempt = 1; attempt < 5; attempt++) {
      review.registerFailedConfirmation(LocalDateTime.now());
    }
    mockMediumRiskConfirmation(review);
    when(accountRepository.findByBankCodeAndAccountNumber("200", recipient.getAccountNumber()))
        .thenReturn(Optional.of(recipient));
    when(passwordEncoder.matches("111111", "hash")).thenReturn(false);

    TransferResponse response = transferService.transfer(1L, request(10_000_000L, true));

    assertThat(response.result()).isEqualTo(TransferResult.BLOCKED);
    assertThat(response.riskLevel()).isEqualTo(RiskLevel.HIGH);
    assertThat(response.riskSignals()).containsExactly(RiskSignal.PIN_CONFIRMATION_FAILED);
    assertThat(review.getConfirmationAttempts()).isEqualTo(5);
    verify(riskAssessmentRepository).save(review);
    assertThat(source.getBalance()).isEqualTo(20_000_000L);
  }

  @Test
  void convertsRiskReviewToBlockedOnFifthFailedConfirmation() {
    TransferRiskAssessment review = mediumReview(LocalDateTime.now().plusMinutes(5));

    for (int attempt = 1; attempt <= 5; attempt++) {
      assertThat(review.registerFailedConfirmation(LocalDateTime.now())).isEqualTo(attempt == 5);
    }

    assertThat(review.getConfirmationAttempts()).isEqualTo(5);
    assertThat(review.getResult()).isEqualTo(TransferResult.BLOCKED);
    assertThat(review.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
    assertThat(review.getRiskSignals()).contains(RiskSignal.PIN_CONFIRMATION_FAILED.name());
    assertThat(review.isConsumed()).isTrue();
  }

  @Test
  void rejectsExpiredMediumRiskReview() {
    mockMediumRiskConfirmation(mediumReview(LocalDateTime.now().minusSeconds(1)));

    assertThatThrownBy(() -> transferService.transfer(1L, request(10_000_000L, true)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Risk review has expired");
  }

  @Test
  void rejectsAlreadyConsumedMediumRiskReview() {
    TransferRiskAssessment review = mediumReview(LocalDateTime.now().plusMinutes(5));
    review.confirmAndConsume(LocalDateTime.now());
    mockMediumRiskConfirmation(review);

    assertThatThrownBy(() -> transferService.transfer(1L, request(10_000_000L, true)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Risk review was already used");
  }

  @Test
  void rejectsTransferOverDailyLimit() {
    mockAccounts();
    when(transactionRepository.sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class)))
        .thenReturn(499_500_000L);

    assertThatThrownBy(() -> transferService.transfer(1L, request(1_000_000L, false)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Daily transfer limit exceeded");
  }

  private void mockAccounts() {
    mockAccountLookups();
    mockConfirmedAccounts();
  }

  private void mockConfirmedAccounts() {
    when(accountRepository.findByBankCodeAndAccountNumber("200", recipient.getAccountNumber()))
        .thenReturn(Optional.of(recipient));
    when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(source));
    when(accountRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(recipient));
    when(transactionRepository.findFirstByTransferIdAndTransactionType(
            "11111111-1111-1111-1111-111111111111", TransactionType.WITHDRAW))
        .thenReturn(Optional.empty());
  }

  private void mockAccountLookups() {
    when(accountRepository.findByUserIdAndAccountNumber(1L, source.getAccountNumber()))
        .thenReturn(Optional.of(source));
  }

  private void mockKnownRecipient() {
    when(transactionRepository.countTransfersToRecipientSince(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(2L),
            org.mockito.ArgumentMatchers.eq("200-01-000001"),
            org.mockito.ArgumentMatchers.eq(TransactionType.WITHDRAW),
            org.mockito.ArgumentMatchers.eq(TransactionStatus.COMPLETED),
            any(LocalDateTime.class)))
        .thenReturn(2L, 0L);
  }

  private void mockMediumRiskConfirmation(TransferRiskAssessment review) {
    when(riskAssessmentRepository.findFirstByTransferIdAndResultOrderByIdDesc(
            "11111111-1111-1111-1111-111111111111", TransferResult.REVIEW_REQUIRED))
        .thenReturn(Optional.of(review));
  }

  private TransferRiskAssessment mediumReview(LocalDateTime expiresAt) {
    return TransferRiskAssessment.builder()
        .id(99L)
        .sourceAccount(source)
        .transferId("11111111-1111-1111-1111-111111111111")
        .recipientBankCode("200")
        .recipientAccountNumber(recipient.getAccountNumber())
        .amount(10_000_000L)
        .riskLevel(RiskLevel.MEDIUM)
        .riskSignals("LARGE_AMOUNT")
        .customerConfirmed(false)
        .expiresAt(expiresAt)
        .result(TransferResult.REVIEW_REQUIRED)
        .build();
  }

  private TransferRequest request(long amount, boolean confirmed) {
    return new TransferRequest(
        source.getAccountNumber(),
        "200",
        recipient.getAccountNumber(),
        amount,
        "생활비",
        TransactionChannel.MOBILE,
        "11111111-1111-1111-1111-111111111111",
        confirmed,
        confirmed ? "111111" : null);
  }

  private Account account(
      Long id, Long userId, Bank bank, String accountNumber, String holder, long balance) {
    return Account.builder()
        .id(id)
        .user(
            User.builder()
                .id(userId)
                .name(holder)
                .ci("ci-" + userId)
                .phone("010" + userId)
                .pinHash("hash")
                .build())
        .bank(bank)
        .accountNumber(accountNumber)
        .holder(holder)
        .balance(balance)
        .status(AccountStatus.ACTIVE)
        .build();
  }
}

package com.inshort.be.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.inshort.be.account.entity.Account;
import com.inshort.be.account.enums.AccountStatus;
import com.inshort.be.account.repository.AccountRepository;
import com.inshort.be.bank.entity.Bank;
import com.inshort.be.transaction.entity.Transaction;
import com.inshort.be.transaction.enums.TransactionChannel;
import com.inshort.be.transaction.enums.TransactionStatus;
import com.inshort.be.transaction.enums.TransactionType;
import com.inshort.be.transaction.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TransferServiceTests {

  @Mock private AccountRepository accountRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private TransferRiskAssessmentRepository riskAssessmentRepository;

  private TransferService transferService;
  private Account source;
  private Account recipient;

  @BeforeEach
  void setUp() {
    Bank sourceBank = Bank.builder().id(1L).code("100").name("한마디은행").build();
    Bank recipientBank = Bank.builder().id(2L).code("200").name("다온은행").build();
    source = account(10L, sourceBank, "100-01-000001", "김한마디", 20_000_000L);
    recipient = account(20L, recipientBank, "200-01-000001", "박다온", 1_000_000L);
    transferService =
        new TransferService(accountRepository, transactionRepository, riskAssessmentRepository);
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
    when(transactionRepository
            .existsByAccountIdAndCounterpartyBankIdAndCounterpartyAccountAndTransactionType(
                10L, 2L, "200-01-000001", TransactionType.WITHDRAW))
        .thenReturn(true);

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
  void requiresReviewForLargeTransferToNewRecipientWithoutChangingBalances() {
    mockAccounts();
    when(transactionRepository.sumAmountSince(
            any(),
            any(TransactionType.class),
            any(TransactionStatus.class),
            any(LocalDateTime.class)))
        .thenReturn(0L);

    TransferResponse response = transferService.transfer(1L, request(10_000_000L, false));

    assertThat(response.result()).isEqualTo(TransferResult.REVIEW_REQUIRED);
    assertThat(response.riskLevel()).isEqualTo(RiskLevel.HIGH);
    assertThat(response.riskSignals())
        .containsExactly(RiskSignal.LARGE_AMOUNT, RiskSignal.NEW_RECIPIENT);
    assertThat(source.getBalance()).isEqualTo(20_000_000L);
    verify(riskAssessmentRepository).save(any());
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

    assertThatThrownBy(() -> transferService.transfer(1L, request(1_000_000L, true)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Daily transfer limit exceeded");
  }

  private void mockAccounts() {
    when(accountRepository.findByUserIdAndAccountNumber(1L, source.getAccountNumber()))
        .thenReturn(Optional.of(source));
    when(accountRepository.findByBankCodeAndAccountNumber("200", recipient.getAccountNumber()))
        .thenReturn(Optional.of(recipient));
    when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(source));
    when(accountRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(recipient));
    when(transactionRepository.findFirstByTransferIdAndTransactionType(
            "11111111-1111-1111-1111-111111111111", TransactionType.WITHDRAW))
        .thenReturn(Optional.empty());
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
        confirmed);
  }

  private Account account(Long id, Bank bank, String accountNumber, String holder, long balance) {
    return Account.builder()
        .id(id)
        .bank(bank)
        .accountNumber(accountNumber)
        .holder(holder)
        .balance(balance)
        .status(AccountStatus.ACTIVE)
        .build();
  }
}

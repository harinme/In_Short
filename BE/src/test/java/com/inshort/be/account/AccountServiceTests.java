package com.inshort.be.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.inshort.be.account.dto.AccountListResponse;
import com.inshort.be.account.entity.Account;
import com.inshort.be.account.enums.AccountStatus;
import com.inshort.be.account.repository.AccountRepository;
import com.inshort.be.bank.entity.Bank;
import com.inshort.be.transaction.repository.TransactionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTests {

  @Mock private AccountRepository accountRepository;

  @Mock private TransactionRepository transactionRepository;

  @InjectMocks private AccountService accountService;

  @Test
  void returnsAuthenticatedUsersAccountsAndTotalBalance() {
    Bank bank = Bank.builder().name("한마디은행").code("100").build();
    Account first = account(bank, "100-01-000001", 1_250_000L);
    Account second = account(bank, "100-01-000002", 3_480_500L);
    when(accountRepository.findAllByUserIdOrderByIdAsc(1L)).thenReturn(List.of(first, second));

    AccountListResponse response = accountService.findAccounts(1L);

    assertThat(response.totalBalance()).isEqualTo(4_730_500L);
    assertThat(response.accounts()).hasSize(2);
    assertThat(response.accounts().getFirst().bankName()).isEqualTo("한마디은행");
  }

  @Test
  void returnsAccountOwnedByAuthenticatedUser() {
    Account account =
        account(Bank.builder().name("한마디은행").code("100").build(), "100-01-000001", 10L);
    when(accountRepository.findByUserIdAndAccountNumber(1L, "100-01-000001"))
        .thenReturn(Optional.of(account));

    assertThat(accountService.findAccount(1L, "100-01-000001").accountNumber())
        .isEqualTo("100-01-000001");
  }

  @Test
  void returnsNotFoundWhenAccountIsNotOwnedByAuthenticatedUser() {
    when(accountRepository.findByUserIdAndAccountNumber(1L, "other")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.findAccount(1L, "other"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404 NOT_FOUND");
  }

  @Test
  void rejectsTransactionPeriodWhenFromIsAfterTo() {
    assertThatThrownBy(
            () ->
                accountService.findTransactions(
                    1L,
                    "100-01-000001",
                    LocalDate.of(2026, 8, 20),
                    LocalDate.of(2026, 8, 1),
                    null,
                    null,
                    0,
                    20))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400 BAD_REQUEST");
  }

  @Test
  void rejectsTransactionPeriodLongerThanOneYear() {
    assertThatThrownBy(
            () ->
                accountService.findTransactions(
                    1L,
                    "100-01-000001",
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 8, 1),
                    null,
                    null,
                    0,
                    20))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400 BAD_REQUEST");
  }

  @Test
  void rejectsInvalidTransactionPageSize() {
    assertThatThrownBy(
            () ->
                accountService.findTransactions(
                    1L,
                    "100-01-000001",
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 19),
                    null,
                    null,
                    0,
                    101))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400 BAD_REQUEST");
  }

  private Account account(Bank bank, String accountNumber, long balance) {
    return Account.builder()
        .bank(bank)
        .accountNumber(accountNumber)
        .holder("김한마디")
        .alias("생활비 통장")
        .balance(balance)
        .status(AccountStatus.ACTIVE)
        .build();
  }
}

package com.inshort.be.account;

import com.inshort.be.account.dto.AccountListResponse;
import com.inshort.be.account.dto.AccountResponse;
import com.inshort.be.account.dto.TransactionListResponse;
import com.inshort.be.account.dto.TransactionDetailResponse;
import com.inshort.be.account.dto.TransactionResponse;
import com.inshort.be.account.entity.Account;
import com.inshort.be.account.repository.AccountRepository;
import com.inshort.be.transaction.repository.TransactionRepository;
import com.inshort.be.transaction.enums.TransactionStatus;
import com.inshort.be.transaction.enums.TransactionType;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AccountService {

  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;

  public AccountService(
      AccountRepository accountRepository, TransactionRepository transactionRepository) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
  }

  public AccountListResponse findAccounts(Long userId) {
    List<AccountResponse> accounts =
        accountRepository.findAllByUserIdOrderByIdAsc(userId).stream()
            .map(AccountResponse::from)
            .toList();
    long totalBalance = accounts.stream().mapToLong(AccountResponse::balance).sum();
    return new AccountListResponse(totalBalance, accounts);
  }

  public AccountResponse findAccount(Long userId, String accountNumber) {
    return AccountResponse.from(findOwnedAccount(userId, accountNumber));
  }

  public TransactionListResponse findTransactions(
      Long userId,
      String accountNumber,
      LocalDate requestedFrom,
      LocalDate requestedTo,
      TransactionType type,
      TransactionStatus status,
      int page,
      int size) {
    LocalDate to = requestedTo == null ? LocalDate.now() : requestedTo;
    LocalDate from = requestedFrom == null ? to.minusMonths(3) : requestedFrom;
    validatePeriod(from, to);
    validatePagination(page, size);

    Account account = findOwnedAccount(userId, accountNumber);
    Page<com.inshort.be.transaction.entity.Transaction> transactionPage =
        transactionRepository
            .findHistory(
                account.getId(),
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                type,
                status,
                PageRequest.of(
                    page,
                    size,
                    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    List<TransactionResponse> transactions =
        transactionPage
            .stream()
            .map(TransactionResponse::from)
            .toList();
    return new TransactionListResponse(
        accountNumber,
        from,
        to,
        page,
        size,
        transactionPage.getNumberOfElements(),
        transactionPage.getTotalElements(),
        transactionPage.hasNext(),
        transactionPage.hasNext() ? page + 1 : null,
        transactions);
  }

  public TransactionDetailResponse findTransaction(
      Long userId, String accountNumber, Long transactionId) {
    Account account = findOwnedAccount(userId, accountNumber);
    return transactionRepository
        .findByIdAndAccountId(transactionId, account.getId())
        .map(TransactionDetailResponse::from)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
  }

  private Account findOwnedAccount(Long userId, String accountNumber) {
    return accountRepository
        .findByUserIdAndAccountNumber(userId, accountNumber)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
  }

  private void validatePeriod(LocalDate from, LocalDate to) {
    if (from.isAfter(to)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must not be after to");
    }
    if (ChronoUnit.DAYS.between(from, to) > 366) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range must not exceed one year");
    }
  }

  private void validatePagination(int page, int size) {
    if (page < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must not be negative");
    }
    if (size < 1 || size > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100");
    }
  }
}

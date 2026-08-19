package com.inshort.be.account;

import com.inshort.be.account.dto.AccountListResponse;
import com.inshort.be.account.dto.AccountResponse;
import com.inshort.be.account.dto.TransactionListResponse;
import com.inshort.be.account.dto.TransactionDetailResponse;
import java.time.LocalDate;
import com.inshort.be.transaction.enums.TransactionStatus;
import com.inshort.be.transaction.enums.TransactionType;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  public AccountListResponse accounts(Authentication authentication) {
    return accountService.findAccounts(authenticatedUserId(authentication));
  }

  @GetMapping("/{accountNumber}")
  public AccountResponse account(
      Authentication authentication, @PathVariable String accountNumber) {
    return accountService.findAccount(authenticatedUserId(authentication), accountNumber);
  }

  @GetMapping("/{accountNumber}/transactions")
  public TransactionListResponse transactions(
      Authentication authentication,
      @PathVariable String accountNumber,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) TransactionType type,
      @RequestParam(required = false) TransactionStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return accountService.findTransactions(
        authenticatedUserId(authentication), accountNumber, from, to, type, status, page, size);
  }

  @GetMapping("/{accountNumber}/transactions/{transactionId}")
  public TransactionDetailResponse transaction(
      Authentication authentication,
      @PathVariable String accountNumber,
      @PathVariable Long transactionId) {
    return accountService.findTransaction(
        authenticatedUserId(authentication), accountNumber, transactionId);
  }

  private Long authenticatedUserId(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    return userId;
  }
}

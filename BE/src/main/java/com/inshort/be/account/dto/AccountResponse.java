package com.inshort.be.account.dto;

import com.inshort.be.account.entity.Account;
import com.inshort.be.account.enums.AccountStatus;

public record AccountResponse(
    String accountNumber,
    String bankName,
    String bankCode,
    String holder,
    String alias,
    long balance,
    AccountStatus status) {

  public static AccountResponse from(Account account) {
    return new AccountResponse(
        account.getAccountNumber(),
        account.getBank().getName(),
        account.getBank().getCode(),
        account.getHolder(),
        account.getAlias(),
        account.getBalance(),
        account.getStatus());
  }
}

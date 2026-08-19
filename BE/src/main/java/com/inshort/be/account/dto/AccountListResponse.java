package com.inshort.be.account.dto;

import java.util.List;

public record AccountListResponse(long totalBalance, List<AccountResponse> accounts) {

  public AccountListResponse {
    accounts = List.copyOf(accounts);
  }
}

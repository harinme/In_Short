package com.inshort.be.transfer;

import com.inshort.be.account.entity.Account;

public record RecipientResponse(
    String bankCode,
    String bankName,
    String accountNumber,
    String holder,
    boolean relationshipEligible) {

  static RecipientResponse from(Account account, Long requestingUserId) {
    return new RecipientResponse(
        account.getBank().getCode(),
        account.getBank().getName(),
        account.getAccountNumber(),
        account.getHolder(),
        !account.getUser().getId().equals(requestingUserId));
  }
}

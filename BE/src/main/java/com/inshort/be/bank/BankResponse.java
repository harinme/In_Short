package com.inshort.be.bank;

import com.inshort.be.bank.entity.Bank;

public record BankResponse(String code, String name) {
  static BankResponse from(Bank bank) {
    return new BankResponse(bank.getCode(), bank.getName());
  }
}

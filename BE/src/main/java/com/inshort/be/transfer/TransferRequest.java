package com.inshort.be.transfer;

import com.inshort.be.transaction.enums.TransactionChannel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TransferRequest(
    @NotBlank @Pattern(regexp = "[0-9-]{8,30}") String sourceAccountNumber,
    @NotBlank @Size(max = 10) String recipientBankCode,
    @NotBlank @Pattern(regexp = "[0-9-]{8,30}") String recipientAccountNumber,
    @Min(1) @Max(100_000_000) long amount,
    @Size(max = 100) String memo,
    @NotNull TransactionChannel channel,
    @NotBlank @Pattern(regexp = "[0-9a-fA-F-]{36}") String requestId,
    boolean riskConfirmed) {

  @AssertTrue(message = "Source and recipient accounts must be different")
  public boolean isDifferentAccount() {
    return !sourceAccountNumber.equals(recipientAccountNumber);
  }
}

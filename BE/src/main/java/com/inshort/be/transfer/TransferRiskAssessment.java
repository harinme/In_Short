package com.inshort.be.transfer;

import com.inshort.be.account.entity.Account;
import com.inshort.be.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "transfer_risk_assessment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TransferRiskAssessment extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_account_id", nullable = false)
  private Account sourceAccount;

  @Column(name = "transfer_id", nullable = false, length = 36)
  private String transferId;

  @Column(name = "recipient_bank_code", nullable = false, length = 10)
  private String recipientBankCode;

  @Column(name = "recipient_account_number", nullable = false, length = 30)
  private String recipientAccountNumber;

  @Column(nullable = false)
  private Long amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_level", nullable = false, length = 10)
  private RiskLevel riskLevel;

  @Column(name = "risk_signals", nullable = false, length = 255)
  private String riskSignals;

  @Column(name = "customer_confirmed", nullable = false)
  private Boolean customerConfirmed;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  @Column(name = "consumed_at")
  private LocalDateTime consumedAt;

  @Column(name = "confirmation_attempts", nullable = false)
  @Builder.Default
  private Integer confirmationAttempts = 0;

  @Column(name = "retention_until", nullable = false)
  private LocalDateTime retentionUntil;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TransferResult result;

  public static TransferRiskAssessment of(
      Account source,
      Account recipient,
      TransferRequest request,
      RiskLevel level,
      List<RiskSignal> signals,
      TransferResult result) {
    return TransferRiskAssessment.builder()
        .sourceAccount(source)
        .transferId(request.requestId().toLowerCase())
        .recipientBankCode(recipient.getBank().getCode())
        .recipientAccountNumber(recipient.getAccountNumber())
        .amount(request.amount())
        .riskLevel(level)
        .riskSignals(
            signals.stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse(""))
        .customerConfirmed(request.riskConfirmed())
        .expiresAt(
            result == TransferResult.REVIEW_REQUIRED ? LocalDateTime.now().plusMinutes(5) : null)
        .retentionUntil(LocalDateTime.now().plusYears(5))
        .result(result)
        .build();
  }

  public boolean isExpired(LocalDateTime now) {
    return expiresAt == null || !expiresAt.isAfter(now);
  }

  public boolean isConsumed() {
    return consumedAt != null;
  }

  public void confirmAndConsume(LocalDateTime now) {
    customerConfirmed = true;
    confirmedAt = now;
    consumedAt = now;
  }

  public boolean registerFailedConfirmation(LocalDateTime now) {
    confirmationAttempts++;
    if (confirmationAttempts >= 5) {
      riskLevel = RiskLevel.HIGH;
      if (!riskSignals.contains(RiskSignal.PIN_CONFIRMATION_FAILED.name())) {
        riskSignals =
            riskSignals.isBlank()
                ? RiskSignal.PIN_CONFIRMATION_FAILED.name()
                : riskSignals + "," + RiskSignal.PIN_CONFIRMATION_FAILED.name();
      }
      result = TransferResult.BLOCKED;
      consumedAt = now;
      return true;
    }
    return false;
  }
}

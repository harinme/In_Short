package com.inshort.be.transfer;

import com.inshort.be.account.entity.Account;
import com.inshort.be.global.entity.BaseEntity;
import jakarta.persistence.*;
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
        .result(result)
        .build();
  }
}

package com.inshort.be.transaction.entity;

import com.inshort.be.account.entity.Account;
import com.inshort.be.bank.entity.Bank;
import com.inshort.be.global.entity.BaseEntity;
import com.inshort.be.transaction.enums.TransactionType;
import com.inshort.be.transaction.enums.TransactionChannel;
import com.inshort.be.transaction.enums.TransactionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 거래가 발생한 내 계좌 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  /** 상대 은행 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "counterparty_bank_id", nullable = false)
  private Bank counterpartyBank;

  /** 거래 종류 */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType transactionType;

  /** 거래 처리 상태 */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TransactionStatus status;

  /** 거래 금액 */
  @Column(nullable = false)
  private Long amount;

  /** 거래 수수료 */
  @Column(nullable = false)
  private Long fee;

  /** 거래가 요청된 채널 */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TransactionChannel channel;

  /** 동일 송금에서 발생한 출금·입금 원장 행을 연결하는 식별값 */
  @Column(name = "transfer_id", length = 36)
  private String transferId;

  /** 고객에게 노출하는 거래 고유번호 */
  @Column(name = "reference_number", nullable = false, unique = true, length = 40)
  private String referenceNumber;

  /** 거래 후 잔액 */
  @Column(nullable = false)
  private Long balanceAfter;

  /** 상대 예금주 */
  @Column(nullable = false, length = 30)
  private String counterpartyName;

  /** 상대 계좌번호 */
  @Column(nullable = false, length = 30)
  private String counterpartyAccount;

  /** 거래 메모 */
  @Column(length = 100)
  private String memo;

  /** 거래 취소 시각 */
  @Column(name = "canceled_at")
  private LocalDateTime canceledAt;
}

package com.inshort.be.transaction.entity;

import com.inshort.be.account.entity.Account;
import com.inshort.be.bank.entity.Bank;
import com.inshort.be.global.entity.BaseEntity;
import com.inshort.be.transaction.enums.TransactionType;
import jakarta.persistence.*;
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

  /** 거래 금액 */
  @Column(nullable = false)
  private Long amount;

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
}

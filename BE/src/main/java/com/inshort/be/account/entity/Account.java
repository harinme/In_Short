package com.inshort.be.account.entity;

import com.inshort.be.account.enums.AccountStatus;
import com.inshort.be.bank.entity.Bank;
import com.inshort.be.global.entity.BaseEntity;
import com.inshort.be.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 계좌 소유자 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** 계좌가 속한 은행 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bank_id", nullable = false)
  private Bank bank;

  /** 계좌번호 */
  @Column(name = "account_number", nullable = false, unique = true, length = 30)
  private String accountNumber;

  /** 예금주 */
  @Column(nullable = false, length = 30)
  private String holder;

  /** 사용자가 계좌를 구분하기 위해 지정하는 선택 별칭 */
  @Column(length = 30)
  private String alias;

  /** 현재 잔액 */
  @Column(nullable = false)
  private Long balance;

  /** 계좌 상태 */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountStatus status;

  public void withdraw(long amount) {
    if (amount < 1 || balance < amount) {
      throw new IllegalArgumentException("Insufficient balance");
    }
    balance -= amount;
  }

  public void deposit(long amount) {
    if (amount < 1) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    balance += amount;
  }
}

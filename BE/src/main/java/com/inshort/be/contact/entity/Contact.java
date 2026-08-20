package com.inshort.be.contact.entity;

import com.inshort.be.bank.entity.Bank;
import com.inshort.be.global.entity.BaseEntity;
import com.inshort.be.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "contact",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_contact_user_bank_account",
            columnNames = {"user_id", "bank_id", "account_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Contact extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 연락처를 소유한 사용자 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** 수취인 은행 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bank_id", nullable = false)
  private Bank bank;

  /** AI가 기억할 별명 ex) 엄마, 영희, 회사 */
  @Column(nullable = false, length = 30)
  private String alias;

  /** 실제 예금주명 */
  @Column(nullable = false, length = 30)
  private String recipientName;

  /** 계좌번호 */
  @Column(name = "account_number", nullable = false, length = 30)
  private String accountNumber;

  @Column(name = "is_favorite", nullable = false)
  @Builder.Default
  private Boolean favorite = false;

  public void update(String alias, boolean favorite) {
    this.alias = alias;
    this.favorite = favorite;
  }
}

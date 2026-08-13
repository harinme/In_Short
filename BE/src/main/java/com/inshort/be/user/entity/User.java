package com.inshort.be.user.entity;

import com.inshort.be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 30)
  private String name;

  @Column(nullable = false, unique = true, length = 100)
  private String ci;

  @Column(nullable = false, unique = true, length = 20)
  private String phone;

  /** BCrypt로 해시한 6자리 간편 비밀번호. 평문 PIN은 저장하지 않는다. */
  @Column(name = "pin_hash", nullable = false, length = 60)
  private String pinHash;
}

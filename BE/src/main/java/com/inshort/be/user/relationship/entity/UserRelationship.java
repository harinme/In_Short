package com.inshort.be.user.relationship.entity;

import com.inshort.be.global.entity.BaseEntity;
import com.inshort.be.user.entity.User;
import com.inshort.be.user.relationship.enums.RelationshipType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "user_relationships",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_relationships_direction",
            columnNames = {"user_id", "related_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserRelationship extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 관계를 조회하는 기준 사용자 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** 기준 사용자와 관계를 맺은 상대 사용자 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "related_user_id", nullable = false)
  private User relatedUser;

  /** 기준 사용자의 관점에서 본 상대 사용자의 관계 */
  @Enumerated(EnumType.STRING)
  @Column(name = "relationship_type", nullable = false, length = 20)
  private RelationshipType relationshipType;

  public void changeType(RelationshipType relationshipType) {
    this.relationshipType = relationshipType;
  }
}

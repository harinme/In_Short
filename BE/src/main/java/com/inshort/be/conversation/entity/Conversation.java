package com.inshort.be.conversation.entity;

import com.inshort.be.conversation.enums.ConversationIntent;
import com.inshort.be.conversation.enums.ConversationRole;
import com.inshort.be.global.entity.BaseEntity;
import com.inshort.be.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "conversation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Conversation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 대화 사용자 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** 대화 세션 ID */
  @Column(nullable = false, length = 100)
  private String sessionId;

  /** USER / ASSISTANT */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ConversationRole role;

  /** AI가 판단한 의도 */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ConversationIntent intent;

  /** 실제 대화 내용 */
  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;
}

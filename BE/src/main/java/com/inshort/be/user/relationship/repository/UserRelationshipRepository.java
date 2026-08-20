package com.inshort.be.user.relationship.repository;

import com.inshort.be.user.relationship.entity.UserRelationship;
import com.inshort.be.user.relationship.enums.RelationshipType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRelationshipRepository extends JpaRepository<UserRelationship, Long> {

  List<UserRelationship> findByUserIdAndRelationshipType(
      Long userId, RelationshipType relationshipType);

  Optional<UserRelationship> findByUserIdAndRelatedUserId(Long userId, Long relatedUserId);
}

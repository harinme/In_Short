package com.inshort.be.transaction.repository;

import com.inshort.be.transaction.entity.Transaction;
import com.inshort.be.transaction.enums.TransactionStatus;
import com.inshort.be.transaction.enums.TransactionType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  @EntityGraph(attributePaths = "counterpartyBank")
  @Query(
      """
      SELECT transaction
      FROM Transaction transaction
      WHERE transaction.account.id = :accountId
        AND transaction.createdAt >= :from
        AND transaction.createdAt < :toExclusive
        AND (:type IS NULL OR transaction.transactionType = :type)
        AND (:status IS NULL OR transaction.status = :status)
      """)
  Page<Transaction> findHistory(
      @Param("accountId") Long accountId,
      @Param("from") LocalDateTime from,
      @Param("toExclusive") LocalDateTime toExclusive,
      @Param("type") TransactionType type,
      @Param("status") TransactionStatus status,
      Pageable pageable);

  @EntityGraph(attributePaths = {"account", "counterpartyBank"})
  Optional<Transaction> findByIdAndAccountId(Long transactionId, Long accountId);
}

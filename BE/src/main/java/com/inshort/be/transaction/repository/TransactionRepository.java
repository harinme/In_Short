package com.inshort.be.transaction.repository;

import com.inshort.be.transaction.entity.Transaction;
import com.inshort.be.transaction.enums.TransactionStatus;
import com.inshort.be.transaction.enums.TransactionChannel;
import com.inshort.be.transaction.enums.TransactionType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

  @Lock(LockModeType.PESSIMISTIC_READ)
  Optional<Transaction> findFirstByTransferIdAndTransactionType(
      String transferId, TransactionType transactionType);

  boolean existsByAccountIdAndCounterpartyBankIdAndCounterpartyAccountAndTransactionType(
      Long accountId, Long bankId, String accountNumber, TransactionType transactionType);

  @Query(
      """
      select coalesce(sum(t.amount), 0) from Transaction t
      where t.account.id = :accountId
        and t.transactionType = :type
        and t.status = :status
        and t.createdAt >= :from
      """)
  long sumAmountSince(
      @Param("accountId") Long accountId,
      @Param("type") TransactionType type,
      @Param("status") TransactionStatus status,
      @Param("from") LocalDateTime from);

  @EntityGraph(attributePaths = "counterpartyBank")
  @Query(
      """
      select t from Transaction t
      where t.account.user.id = :userId
        and t.transactionType = :type
        and t.status = :status
        and t.channel <> :excludedChannel
      order by t.createdAt desc, t.id desc
      """)
  Page<Transaction> findRecentCounterparties(
      @Param("userId") Long userId,
      @Param("type") TransactionType type,
      @Param("status") TransactionStatus status,
      @Param("excludedChannel") TransactionChannel excludedChannel,
      Pageable pageable);
}

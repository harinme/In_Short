package com.inshort.be.account.repository;

import com.inshort.be.account.entity.Account;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

  @EntityGraph(attributePaths = "bank")
  List<Account> findAllByUserIdOrderByIdAsc(Long userId);

  @EntityGraph(attributePaths = "bank")
  Optional<Account> findByUserIdAndAccountNumber(Long userId, String accountNumber);

  Optional<Account> findByBankCodeAndAccountNumber(String bankCode, String accountNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Account a where a.id = :id")
  Optional<Account> findByIdForUpdate(@Param("id") Long id);
}

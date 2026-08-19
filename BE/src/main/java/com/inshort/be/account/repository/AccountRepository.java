package com.inshort.be.account.repository;

import com.inshort.be.account.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

  @EntityGraph(attributePaths = "bank")
  List<Account> findAllByUserIdOrderByIdAsc(Long userId);

  @EntityGraph(attributePaths = "bank")
  Optional<Account> findByUserIdAndAccountNumber(Long userId, String accountNumber);
}

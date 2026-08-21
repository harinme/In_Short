package com.inshort.be.transfer;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRiskAssessmentRepository
    extends JpaRepository<TransferRiskAssessment, Long> {

  @EntityGraph(attributePaths = {"sourceAccount", "sourceAccount.user"})
  Optional<TransferRiskAssessment> findFirstByTransferIdAndResultOrderByIdDesc(
      String transferId, TransferResult result);

  @Query(
      """
      select case when count(risk) > 0 then true else false end
      from TransferRiskAssessment risk
      where risk.sourceAccount.user.id = :userId
        and risk.recipientBankCode = :bankCode
        and risk.recipientAccountNumber = :accountNumber
        and risk.result = :result
        and risk.createdAt >= :from
      """)
  boolean existsRecentBlockedRecipient(
      @Param("userId") Long userId,
      @Param("bankCode") String bankCode,
      @Param("accountNumber") String accountNumber,
      @Param("result") TransferResult result,
      @Param("from") LocalDateTime from);
}

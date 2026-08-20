package com.inshort.be.contact.repository;

import com.inshort.be.contact.entity.Contact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {

  @EntityGraph(attributePaths = "bank")
  List<Contact> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

  Optional<Contact> findByUserIdAndBankIdAndAccountNumber(
      Long userId, Long bankId, String accountNumber);

  Optional<Contact> findByIdAndUserId(Long id, Long userId);
}

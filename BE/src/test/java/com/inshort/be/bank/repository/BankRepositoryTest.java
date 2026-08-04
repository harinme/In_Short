package com.inshort.be.bank.repository;

import com.inshort.be.bank.entity.Bank;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BankRepositoryTest {

  @Autowired private BankRepository bankRepository;

  @Test
  void saveTest() {

    Bank bank =
        Bank.builder().name("국민은행").code(UUID.randomUUID().toString().substring(0, 10)).build();

    bankRepository.save(bank);
  }
}

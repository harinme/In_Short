package com.inshort.be.bank.repository;

import com.inshort.be.bank.entity.Bank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BankRepositoryTest {

    @Autowired
    private BankRepository bankRepository;

    @Test
    void saveTest() {

        Bank bank = Bank.builder()
                .name("국민은행")
                .code("004")
                .build();

        bankRepository.save(bank);
    }
}
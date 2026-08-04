package com.inshort.be.bank.repository;

import com.inshort.be.bank.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRepository extends JpaRepository<Bank, Long> {

}
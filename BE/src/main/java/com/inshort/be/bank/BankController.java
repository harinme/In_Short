package com.inshort.be.bank;

import com.inshort.be.bank.repository.BankRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banks")
public class BankController {

  private final BankRepository bankRepository;

  public BankController(BankRepository bankRepository) {
    this.bankRepository = bankRepository;
  }

  @GetMapping
  public List<BankResponse> banks() {
    return bankRepository.findAllByOrderByNameAsc().stream().map(BankResponse::from).toList();
  }
}

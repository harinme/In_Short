package com.inshort.be.transfer;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

  private final TransferService transferService;

  public TransferController(TransferService transferService) {
    this.transferService = transferService;
  }

  @GetMapping("/recipient")
  public RecipientResponse recipient(
      Authentication authentication,
      @RequestParam String bankCode,
      @RequestParam String accountNumber) {
    return transferService.findRecipient(
        authenticatedUserId(authentication), bankCode, accountNumber);
  }

  @GetMapping("/recipient-suggestions")
  public List<RecipientSuggestionResponse> recipientSuggestions(Authentication authentication) {
    return transferService.findRecipientSuggestions(authenticatedUserId(authentication));
  }

  @PostMapping
  public TransferResponse transfer(
      Authentication authentication, @Valid @RequestBody TransferRequest request) {
    return transferService.transfer(authenticatedUserId(authentication), request);
  }

  private Long authenticatedUserId(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    return userId;
  }
}

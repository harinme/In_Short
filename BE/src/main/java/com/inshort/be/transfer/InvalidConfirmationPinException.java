package com.inshort.be.transfer;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidConfirmationPinException extends ResponseStatusException {

  public InvalidConfirmationPinException() {
    super(HttpStatus.UNAUTHORIZED, "Invalid confirmation PIN");
  }
}

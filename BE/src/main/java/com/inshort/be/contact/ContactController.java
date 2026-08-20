package com.inshort.be.contact;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

  private final ContactService contactService;

  public ContactController(ContactService contactService) {
    this.contactService = contactService;
  }

  @PostMapping
  public ContactResponse save(
      Authentication authentication, @Valid @RequestBody ContactSaveRequest request) {
    return contactService.save(authenticatedUserId(authentication), request);
  }

  @PatchMapping("/{contactId}/favorite")
  public ContactResponse favorite(
      Authentication authentication,
      @PathVariable Long contactId,
      @RequestParam boolean favorite) {
    return contactService.changeFavorite(
        authenticatedUserId(authentication), contactId, favorite);
  }

  private Long authenticatedUserId(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    return userId;
  }
}

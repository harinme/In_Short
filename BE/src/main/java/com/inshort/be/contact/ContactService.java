package com.inshort.be.contact;

import com.inshort.be.account.entity.Account;
import com.inshort.be.account.repository.AccountRepository;
import com.inshort.be.contact.entity.Contact;
import com.inshort.be.contact.repository.ContactRepository;
import com.inshort.be.user.entity.User;
import com.inshort.be.user.relationship.entity.UserRelationship;
import com.inshort.be.user.relationship.repository.UserRelationshipRepository;
import com.inshort.be.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContactService {

  private final ContactRepository contactRepository;
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final UserRelationshipRepository relationshipRepository;

  public ContactService(
      ContactRepository contactRepository,
      AccountRepository accountRepository,
      UserRepository userRepository,
      UserRelationshipRepository relationshipRepository) {
    this.contactRepository = contactRepository;
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
    this.relationshipRepository = relationshipRepository;
  }

  @Transactional
  public ContactResponse save(Long userId, ContactSaveRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    Account recipient =
        accountRepository
            .findByBankCodeAndAccountNumber(request.bankCode(), request.accountNumber())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Recipient account not found"));
    Contact contact =
        contactRepository
            .findByUserIdAndBankIdAndAccountNumber(
                userId, recipient.getBank().getId(), recipient.getAccountNumber())
            .orElseGet(
                () ->
                    Contact.builder()
                        .user(user)
                        .bank(recipient.getBank())
                        .accountNumber(recipient.getAccountNumber())
                        .recipientName(recipient.getHolder())
                        .alias(request.alias().trim())
                        .favorite(request.favorite())
                        .build());
    contact.update(request.alias().trim(), request.favorite());
    contactRepository.save(contact);

    var relationshipType = request.relationshipType();
    if (relationshipType != null) {
      if (recipient.getUser().getId().equals(userId)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Relationship cannot target the same user");
      }
      UserRelationship relationship =
          relationshipRepository
              .findByUserIdAndRelatedUserId(userId, recipient.getUser().getId())
              .orElseGet(
                  () ->
                      UserRelationship.builder()
                          .user(user)
                          .relatedUser(recipient.getUser())
                          .relationshipType(relationshipType)
                          .build());
      relationship.changeType(relationshipType);
      relationshipRepository.save(relationship);
    }
    return ContactResponse.from(contact, relationshipType);
  }

  @Transactional
  public ContactResponse changeFavorite(Long userId, Long contactId, boolean favorite) {
    Contact contact =
        contactRepository
            .findByIdAndUserId(contactId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
    contact.update(contact.getAlias(), favorite);
    return ContactResponse.from(contact, null);
  }
}

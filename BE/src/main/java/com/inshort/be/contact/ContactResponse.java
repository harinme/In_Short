package com.inshort.be.contact;

import com.inshort.be.contact.entity.Contact;
import com.inshort.be.user.relationship.enums.RelationshipType;

public record ContactResponse(
    Long contactId,
    String bankCode,
    String bankName,
    String accountNumber,
    String recipientName,
    String alias,
    boolean favorite,
    RelationshipType relationshipType) {

  static ContactResponse from(Contact contact, RelationshipType relationshipType) {
    return new ContactResponse(
        contact.getId(),
        contact.getBank().getCode(),
        contact.getBank().getName(),
        contact.getAccountNumber(),
        contact.getRecipientName(),
        contact.getAlias(),
        contact.getFavorite(),
        relationshipType);
  }
}

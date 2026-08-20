package com.inshort.be.transfer;

import com.inshort.be.user.relationship.enums.RelationshipType;

public record RecipientSuggestionResponse(
    String bankCode,
    String bankName,
    String accountNumber,
    String holder,
    String alias,
    boolean favorite,
    boolean saved,
    RelationshipType relationshipType) {}

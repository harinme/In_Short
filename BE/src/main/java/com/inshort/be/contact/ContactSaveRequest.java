package com.inshort.be.contact;

import com.inshort.be.user.relationship.enums.RelationshipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactSaveRequest(
    @NotBlank @Size(max = 10) String bankCode,
    @NotBlank @Size(max = 30) String accountNumber,
    @NotBlank @Size(max = 30) String alias,
    boolean favorite,
    RelationshipType relationshipType) {}

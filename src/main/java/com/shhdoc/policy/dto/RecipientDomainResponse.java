package com.shhdoc.policy.dto;

import com.shhdoc.policy.entity.RecipientDomain;
import com.shhdoc.policy.entity.RecipientScope;

public record RecipientDomainResponse(Long id, String domain, RecipientScope scope) {

    public static RecipientDomainResponse from(RecipientDomain recipientDomain) {
        return new RecipientDomainResponse(recipientDomain.getId(), recipientDomain.getDomain(),
                recipientDomain.getScope());
    }
}

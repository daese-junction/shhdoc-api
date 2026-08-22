package com.shhdoc.policy.service.impl;

import com.shhdoc.common.ApiException;
import com.shhdoc.company.CompanyRepository;
import com.shhdoc.policy.entity.RecipientDomain;
import com.shhdoc.policy.entity.RecipientScope;
import com.shhdoc.policy.repository.RecipientDomainRepository;
import com.shhdoc.policy.service.RecipientDomainService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipientDomainServiceImpl implements RecipientDomainService {

    private final RecipientDomainRepository domainRepository;
    private final CompanyRepository companyRepository;

    @Override
    public List<RecipientDomain> list(Long companyId) {
        return domainRepository.findByCompanyIdOrderByIdAsc(companyId);
    }

    @Override
    @Transactional
    public RecipientDomain create(Long companyId, String domain, RecipientScope scope) {
        validateScope(scope);
        String normalized = normalizeDomain(domain);
        if (domainRepository.existsByCompanyIdAndDomain(companyId, normalized)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 등록된 도메인입니다.");
        }
        return domainRepository.save(new RecipientDomain(
                companyRepository.getReferenceById(companyId), normalized, scope));
    }

    @Override
    @Transactional
    public RecipientDomain update(Long companyId, Long domainId, String domain, RecipientScope scope) {
        validateScope(scope);
        RecipientDomain recipientDomain = findOwned(companyId, domainId);
        String normalized = normalizeDomain(domain);
        if (domainRepository.existsByCompanyIdAndDomainAndIdNot(companyId, normalized, domainId)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 등록된 도메인입니다.");
        }
        recipientDomain.update(normalized, scope);
        return recipientDomain;
    }

    @Override
    @Transactional
    public void delete(Long companyId, Long domainId) {
        domainRepository.delete(findOwned(companyId, domainId));
    }

    private RecipientDomain findOwned(Long companyId, Long domainId) {
        RecipientDomain recipientDomain = domainRepository.findById(domainId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "도메인을 찾을 수 없습니다."));
        if (!recipientDomain.getCompany().getId().equals(companyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "도메인을 찾을 수 없습니다.");
        }
        return recipientDomain;
    }

    /** INTERNAL/EXTERNAL 은 판정 시 파생되는 값이라 등록을 막는다. */
    private static void validateScope(RecipientScope scope) {
        if (scope != RecipientScope.PARTNER && scope != RecipientScope.PERSONAL_EMAIL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "등록 가능한 수신 범위는 PARTNER, PERSONAL_EMAIL 뿐입니다.");
        }
    }

    /** "@Gmail.com " 처럼 들어와도 "gmail.com" 하나로 모은다. */
    private static String normalizeDomain(String domain) {
        return domain.trim().toLowerCase().replaceFirst("^@", "");
    }
}

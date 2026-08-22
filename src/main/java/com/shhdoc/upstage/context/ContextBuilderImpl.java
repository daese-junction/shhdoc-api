package com.shhdoc.upstage.context;

import com.shhdoc.company.Company;
import com.shhdoc.company.CompanyRepository;
import com.shhdoc.policy.entity.RecipientDomain;
import com.shhdoc.policy.entity.RecipientScope;
import com.shhdoc.policy.repository.RecipientDomainRepository;
import com.shhdoc.upstage.document.DocumentAnalysisResult;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.Recipient;
import com.shhdoc.upstage.pipeline.extract.ExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextBuilderImpl implements ContextBuilder {

    private final CompanyRepository companyRepository;
    private final RecipientDomainRepository recipientDomainRepository;

    @Override
    public String resolveRecipientType(MailRequest mail) {
        List<String> recipientAddresses = mail.recipients().stream().map(Recipient::address).toList();
        String recipientType = resolveRecipientType(mail.companyId(), recipientAddresses);
        log.info("[CONTEXT] mailId={} recipientType={}", mail.mailId(), recipientType);
        return recipientType;
    }

    @Override
    public MailContext build(MailRequest mail, DocumentAnalysisResult docResult, String recipientType) {
        ExtractionResult extraction = docResult.extraction();
        List<String> recipientAddresses = mail.recipients().stream().map(Recipient::address).toList();

        return new MailContext(
                mail.senderAddress(),
                recipientAddresses,
                recipientType,
                docResult.classification().category(),
                extraction.matchedSensitiveTypeCodes(),
                extraction.classification(),
                extraction.confidentialityMarking()
        );
    }

    /**
     * 수신자마다 {@code RecipientScope}(INTERNAL/PARTNER/PERSONAL_EMAIL/EXTERNAL)를
     * 구한 뒤, 그 중 가장 위험한(외부에 가까운) 값 하나로 대표한다 — 수신자가 여러 명이면
     * 그 중 가장 느슨한 상대가 실제 유출 위험이라 안전한 쪽(더 엄격한 판정)으로 정한다.
     * {@code RecipientScope}는 선언 순서 자체가 위험도 서열(INTERNAL이 가장 낮음)이다.
     *
     * <p>INTERNAL은 회사의 {@code emailDomain}과 일치하는지로 판단(파생값, 저장 안 함).
     * PARTNER/PERSONAL_EMAIL은 회사가 등록한 {@code RecipientDomain}에서 조회하고,
     * 등록 안 된 도메인은 EXTERNAL로 취급한다.
     */
    private String resolveRecipientType(Long companyId, List<String> recipientAddresses) {
        if (recipientAddresses.isEmpty()) {
            return null;
        }

        String companyDomain = companyRepository.findById(companyId)
                .map(Company::getEmailDomain)
                .orElse(null);
        Map<String, RecipientScope> registeredDomains = recipientDomainRepository
                .findByCompanyIdOrderByIdAsc(companyId).stream()
                .collect(Collectors.toMap(
                        d -> d.getDomain().toLowerCase(), RecipientDomain::getScope, (a, b) -> a));

        RecipientScope worst = recipientAddresses.stream()
                .map(address -> scopeOf(domainOf(address), companyDomain, registeredDomains))
                .max(Comparator.comparingInt(RecipientScope::ordinal))
                .orElseThrow();

        return worst.name().toLowerCase();
    }

    private RecipientScope scopeOf(String domain, String companyDomain, Map<String, RecipientScope> registeredDomains) {
        if (companyDomain != null && companyDomain.equalsIgnoreCase(domain)) {
            return RecipientScope.INTERNAL;
        }
        return registeredDomains.getOrDefault(domain, RecipientScope.EXTERNAL);
    }

    private String domainOf(String address) {
        int at = address.indexOf('@');
        return at < 0 ? "" : address.substring(at + 1).toLowerCase();
    }
}

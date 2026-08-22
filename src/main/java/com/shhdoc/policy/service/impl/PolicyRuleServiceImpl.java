package com.shhdoc.policy.service.impl;

import com.shhdoc.common.ApiException;
import com.shhdoc.company.CompanyRepository;
import com.shhdoc.policy.entity.DocumentCategory;
import com.shhdoc.policy.entity.DocumentType;
import com.shhdoc.policy.entity.PolicyRule;
import com.shhdoc.policy.entity.SendDirection;
import com.shhdoc.policy.entity.SensitiveInfoType;
import com.shhdoc.policy.repository.DocumentCategoryRepository;
import com.shhdoc.policy.repository.DocumentTypeRepository;
import com.shhdoc.policy.repository.PolicyRuleRepository;
import com.shhdoc.policy.repository.SensitiveInfoTypeRepository;
import com.shhdoc.policy.service.PolicyRuleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyRuleServiceImpl implements PolicyRuleService {

    private final PolicyRuleRepository ruleRepository;
    private final DocumentCategoryRepository categoryRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final SensitiveInfoTypeRepository sensitiveTypeRepository;
    private final CompanyRepository companyRepository;

    @Override
    public List<PolicyRule> list(Long companyId) {
        return ruleRepository.findByCompanyIdOrderByIdAsc(companyId);
    }

    @Override
    @Transactional
    public PolicyRule create(Long companyId, RuleData data) {
        validate(data);
        return ruleRepository.save(new PolicyRule(
                companyRepository.getReferenceById(companyId),
                data.name(),
                categoryOrNull(companyId, data.categoryId()),
                documentTypeOrNull(companyId, data.documentTypeId()),
                sensitiveTypeOrNull(companyId, data.sensitiveTypeId()),
                data.classification(),
                data.direction(),
                data.recipientScope(),
                data.action()));
    }

    @Override
    @Transactional
    public PolicyRule update(Long companyId, Long ruleId, RuleData data) {
        validate(data);
        PolicyRule rule = findOwned(companyId, ruleId);
        rule.update(
                data.name(),
                categoryOrNull(companyId, data.categoryId()),
                documentTypeOrNull(companyId, data.documentTypeId()),
                sensitiveTypeOrNull(companyId, data.sensitiveTypeId()),
                data.classification(),
                data.direction(),
                data.recipientScope(),
                data.action());
        return rule;
    }

    @Override
    @Transactional
    public PolicyRule changeEnabled(Long companyId, Long ruleId, boolean enabled) {
        PolicyRule rule = findOwned(companyId, ruleId);
        rule.changeEnabled(enabled);
        return rule;
    }

    @Override
    @Transactional
    public void delete(Long companyId, Long ruleId) {
        ruleRepository.delete(findOwned(companyId, ruleId));
    }

    private static void validate(RuleData data) {
        if (data.direction() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "발송 방향(direction)은 필수입니다.");
        }
        if (data.action() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "판정(action)은 필수입니다.");
        }
        if (data.recipientScope() != null && data.direction() != SendDirection.OUTBOUND) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "수신 범위 조건은 발송 방향이 OUTBOUND 일 때만 지정할 수 있습니다.");
        }
    }

    private PolicyRule findOwned(Long companyId, Long ruleId) {
        PolicyRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "정책 규칙을 찾을 수 없습니다."));
        if (!rule.getCompany().getId().equals(companyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "정책 규칙을 찾을 수 없습니다.");
        }
        return rule;
    }

    private DocumentCategory categoryOrNull(Long companyId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        DocumentCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "대분류를 찾을 수 없습니다."));
        if (!category.getCompany().getId().equals(companyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "대분류를 찾을 수 없습니다.");
        }
        return category;
    }

    private DocumentType documentTypeOrNull(Long companyId, Long documentTypeId) {
        if (documentTypeId == null) {
            return null;
        }
        DocumentType documentType = documentTypeRepository.findById(documentTypeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "문서 유형을 찾을 수 없습니다."));
        if (!documentType.getCompany().getId().equals(companyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "문서 유형을 찾을 수 없습니다.");
        }
        return documentType;
    }

    private SensitiveInfoType sensitiveTypeOrNull(Long companyId, Long sensitiveTypeId) {
        if (sensitiveTypeId == null) {
            return null;
        }
        SensitiveInfoType sensitiveType = sensitiveTypeRepository.findById(sensitiveTypeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "민감정보 유형을 찾을 수 없습니다."));
        if (!sensitiveType.getCompany().getId().equals(companyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "민감정보 유형을 찾을 수 없습니다.");
        }
        return sensitiveType;
    }
}

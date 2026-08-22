package com.shhdoc.policy.service.impl;

import com.shhdoc.common.ApiException;
import com.shhdoc.company.CompanyRepository;
import com.shhdoc.policy.entity.SensitiveInfoType;
import com.shhdoc.policy.repository.PolicyRuleRepository;
import com.shhdoc.policy.repository.SensitiveInfoTypeRepository;
import com.shhdoc.policy.service.SensitiveInfoTypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensitiveInfoTypeServiceImpl implements SensitiveInfoTypeService {

    private final SensitiveInfoTypeRepository sensitiveTypeRepository;
    private final PolicyRuleRepository policyRuleRepository;
    private final CompanyRepository companyRepository;

    @Override
    public List<SensitiveInfoType> list(Long companyId) {
        return sensitiveTypeRepository.findByCompanyIdOrderByIdAsc(companyId);
    }

    @Override
    @Transactional
    public SensitiveInfoType create(Long companyId, String code, String name, String description) {
        if (sensitiveTypeRepository.existsByCompanyIdAndCode(companyId, code)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 존재하는 민감정보 유형 코드입니다.");
        }
        return sensitiveTypeRepository.save(new SensitiveInfoType(
                companyRepository.getReferenceById(companyId), code, name, description));
    }

    @Override
    @Transactional
    public SensitiveInfoType update(Long companyId, Long sensitiveTypeId, String code, String name,
                                    String description) {
        SensitiveInfoType sensitiveType = findOwned(companyId, sensitiveTypeId);
        if (sensitiveTypeRepository.existsByCompanyIdAndCodeAndIdNot(companyId, code, sensitiveTypeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 존재하는 민감정보 유형 코드입니다.");
        }
        sensitiveType.update(code, name, description);
        return sensitiveType;
    }

    @Override
    @Transactional
    public void delete(Long companyId, Long sensitiveTypeId) {
        SensitiveInfoType sensitiveType = findOwned(companyId, sensitiveTypeId);
        if (policyRuleRepository.existsBySensitiveTypeId(sensitiveTypeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "이 민감정보 유형을 조건으로 쓰는 정책 규칙이 있어 삭제할 수 없습니다.");
        }
        sensitiveTypeRepository.delete(sensitiveType);
    }

    private SensitiveInfoType findOwned(Long companyId, Long sensitiveTypeId) {
        SensitiveInfoType sensitiveType = sensitiveTypeRepository.findById(sensitiveTypeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "민감정보 유형을 찾을 수 없습니다."));
        if (!sensitiveType.getCompany().getId().equals(companyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "민감정보 유형을 찾을 수 없습니다.");
        }
        return sensitiveType;
    }
}

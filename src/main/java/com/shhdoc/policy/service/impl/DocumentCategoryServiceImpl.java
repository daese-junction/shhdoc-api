package com.shhdoc.policy.service.impl;

import com.shhdoc.common.ApiException;
import com.shhdoc.company.CompanyRepository;
import com.shhdoc.policy.entity.DocumentCategory;
import com.shhdoc.policy.repository.DocumentCategoryRepository;
import com.shhdoc.policy.repository.DocumentTypeRepository;
import com.shhdoc.policy.repository.PolicyRuleRepository;
import com.shhdoc.policy.service.DocumentCategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentCategoryServiceImpl implements DocumentCategoryService {

    private final DocumentCategoryRepository categoryRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final PolicyRuleRepository policyRuleRepository;
    private final CompanyRepository companyRepository;

    @Override
    public List<DocumentCategory> list(Long companyId) {
        return categoryRepository.findByCompanyIdOrderByIdAsc(companyId);
    }

    @Override
    @Transactional
    public DocumentCategory create(Long companyId, String code, String name) {
        if (categoryRepository.existsByCompanyIdAndCode(companyId, code)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 존재하는 대분류 코드입니다.");
        }
        return categoryRepository.save(
                new DocumentCategory(companyRepository.getReferenceById(companyId), code, name));
    }

    @Override
    @Transactional
    public DocumentCategory update(Long companyId, Long categoryId, String code, String name) {
        DocumentCategory category = findOwned(companyId, categoryId);
        if (categoryRepository.existsByCompanyIdAndCodeAndIdNot(companyId, code, categoryId)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 존재하는 대분류 코드입니다.");
        }
        category.update(code, name);
        return category;
    }

    @Override
    @Transactional
    public void delete(Long companyId, Long categoryId) {
        DocumentCategory category = findOwned(companyId, categoryId);
        if (documentTypeRepository.existsByCategoryId(categoryId)) {
            throw new ApiException(HttpStatus.CONFLICT, "하위 문서 유형이 있어 삭제할 수 없습니다.");
        }
        if (policyRuleRepository.existsByCategoryId(categoryId)) {
            throw new ApiException(HttpStatus.CONFLICT, "이 대분류를 조건으로 쓰는 정책 규칙이 있어 삭제할 수 없습니다.");
        }
        categoryRepository.delete(category);
    }

    /** 남의 회사 데이터는 존재 여부도 숨기려고 404 로 응답한다. */
    private DocumentCategory findOwned(Long companyId, Long categoryId) {
        DocumentCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "대분류를 찾을 수 없습니다."));
        if (!category.getCompany().getId().equals(companyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "대분류를 찾을 수 없습니다.");
        }
        return category;
    }
}

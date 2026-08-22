package com.shhdoc.policy.service.impl;

import com.shhdoc.common.ApiException;
import com.shhdoc.company.CompanyRepository;
import com.shhdoc.policy.entity.DocumentCategory;
import com.shhdoc.policy.entity.DocumentType;
import com.shhdoc.policy.repository.DocumentCategoryRepository;
import com.shhdoc.policy.repository.DocumentTypeRepository;
import com.shhdoc.policy.repository.PolicyRuleRepository;
import com.shhdoc.policy.service.DocumentTypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentTypeServiceImpl implements DocumentTypeService {

    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentCategoryRepository categoryRepository;
    private final PolicyRuleRepository policyRuleRepository;
    private final CompanyRepository companyRepository;

    @Override
    public List<DocumentType> list(Long companyId) {
        return documentTypeRepository.findByCompanyIdOrderByIdAsc(companyId);
    }

    @Override
    @Transactional
    public DocumentType create(Long companyId, Long categoryId, String code, String name, String description) {
        if (documentTypeRepository.existsByCompanyIdAndCode(companyId, code)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 존재하는 문서 유형 코드입니다.");
        }
        DocumentCategory category = findOwnedCategory(companyId, categoryId);
        return documentTypeRepository.save(new DocumentType(
                companyRepository.getReferenceById(companyId), category, code, name, description));
    }

    @Override
    @Transactional
    public DocumentType update(Long companyId, Long documentTypeId, Long categoryId, String code, String name,
                               String description) {
        DocumentType documentType = findOwned(companyId, documentTypeId);
        if (documentTypeRepository.existsByCompanyIdAndCodeAndIdNot(companyId, code, documentTypeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 존재하는 문서 유형 코드입니다.");
        }
        documentType.update(findOwnedCategory(companyId, categoryId), code, name, description);
        return documentType;
    }

    @Override
    @Transactional
    public void delete(Long companyId, Long documentTypeId) {
        DocumentType documentType = findOwned(companyId, documentTypeId);
        if (policyRuleRepository.existsByDocumentTypeId(documentTypeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "이 문서 유형을 조건으로 쓰는 정책 규칙이 있어 삭제할 수 없습니다.");
        }
        documentTypeRepository.delete(documentType);
    }

    private DocumentType findOwned(Long companyId, Long documentTypeId) {
        DocumentType documentType = documentTypeRepository.findById(documentTypeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "문서 유형을 찾을 수 없습니다."));
        if (!documentType.getCompany().getId().equals(companyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "문서 유형을 찾을 수 없습니다.");
        }
        return documentType;
    }

    private DocumentCategory findOwnedCategory(Long companyId, Long categoryId) {
        DocumentCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "대분류를 찾을 수 없습니다."));
        if (!category.getCompany().getId().equals(companyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "대분류를 찾을 수 없습니다.");
        }
        return category;
    }
}

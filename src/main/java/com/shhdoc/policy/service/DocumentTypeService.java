package com.shhdoc.policy.service;

import com.shhdoc.policy.entity.DocumentType;
import java.util.List;

/**
 * 문서 유형 관리.
 * 모든 메서드는 companyId 기준으로 테넌트를 격리한다.
 * description 은 AI 분류 프롬프트에 힌트로 주입되므로 문서의 특징을 구체적으로 적을수록 분류 정확도가 올라간다.
 */
public interface DocumentTypeService {

    /** 회사의 문서 유형 전체 목록. */
    List<DocumentType> list(Long companyId);

    /**
     * 문서 유형 추가.
     *
     * @param categoryId 소속 대분류. 같은 회사의 대분류여야 한다.
     * @throws com.shhdoc.common.ApiException 대분류가 없거나 다른 회사 소속이면, 또는 code 가 중복되면
     */
    DocumentType create(Long companyId, Long categoryId, String code, String name, String description);

    /**
     * 문서 유형 수정. 대분류 이동도 여기서 처리한다.
     *
     * @throws com.shhdoc.common.ApiException 대상·대분류가 없거나 다른 회사 소속이면, 또는 code 가 중복되면
     */
    DocumentType update(Long companyId, Long documentTypeId, Long categoryId, String code, String name,
                        String description);

    /**
     * 문서 유형 삭제.
     *
     * @throws com.shhdoc.common.ApiException 대상이 없거나 다른 회사 소속이면, 또는 정책 규칙이 참조 중이면
     */
    void delete(Long companyId, Long documentTypeId);
}

package com.shhdoc.policy.service;

import com.shhdoc.policy.entity.DocumentCategory;
import java.util.List;

/**
 * 문서 대분류 관리.
 * 모든 메서드는 companyId 기준으로 테넌트를 격리한다. 다른 회사의 데이터는 조회·수정할 수 없다.
 */
public interface DocumentCategoryService {

    /** 회사의 대분류 전체 목록. */
    List<DocumentCategory> list(Long companyId);

    /**
     * 대분류 추가.
     *
     * @throws com.shhdoc.common.ApiException 같은 회사에 동일 code 가 이미 있으면
     */
    DocumentCategory create(Long companyId, String code, String name);

    /**
     * 대분류 수정.
     *
     * @throws com.shhdoc.common.ApiException 대상이 없거나 다른 회사 소속이면, 또는 code 가 중복되면
     */
    DocumentCategory update(Long companyId, Long categoryId, String code, String name);

    /**
     * 대분류 삭제.
     *
     * @throws com.shhdoc.common.ApiException 대상이 없거나 다른 회사 소속이면,
     *         또는 하위 문서 유형이나 정책 규칙이 참조 중이면
     */
    void delete(Long companyId, Long categoryId);
}

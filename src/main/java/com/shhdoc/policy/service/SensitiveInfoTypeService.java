package com.shhdoc.policy.service;

import com.shhdoc.policy.entity.SensitiveInfoType;
import java.util.List;

/**
 * 민감정보 유형 관리.
 * 모든 메서드는 companyId 기준으로 테넌트를 격리한다.
 * description 은 AI 탐지 프롬프트에 힌트로 주입된다.
 */
public interface SensitiveInfoTypeService {

    /** 회사의 민감정보 유형 전체 목록. */
    List<SensitiveInfoType> list(Long companyId);

    /**
     * 민감정보 유형 추가.
     *
     * @throws com.shhdoc.common.ApiException 같은 회사에 동일 code 가 이미 있으면
     */
    SensitiveInfoType create(Long companyId, String code, String name, String description);

    /**
     * 민감정보 유형 수정.
     *
     * @throws com.shhdoc.common.ApiException 대상이 없거나 다른 회사 소속이면, 또는 code 가 중복되면
     */
    SensitiveInfoType update(Long companyId, Long sensitiveTypeId, String code, String name, String description);

    /**
     * 민감정보 유형 삭제.
     *
     * @throws com.shhdoc.common.ApiException 대상이 없거나 다른 회사 소속이면, 또는 정책 규칙이 참조 중이면
     */
    void delete(Long companyId, Long sensitiveTypeId);
}

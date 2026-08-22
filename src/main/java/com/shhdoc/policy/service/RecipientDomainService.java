package com.shhdoc.policy.service;

import com.shhdoc.policy.entity.RecipientDomain;
import com.shhdoc.policy.entity.RecipientScope;
import java.util.List;

/**
 * 수신자 도메인 관리. 수신자 이메일의 도메인을 수신 범위(scope)로 매핑하는 근거 데이터다.
 * 모든 메서드는 companyId 기준으로 테넌트를 격리한다.
 *
 * <p>INTERNAL(회사 도메인과 일치)과 EXTERNAL(어디에도 매핑되지 않음)은 판정 시 파생되는 값이라
 * 저장 대상이 아니다. 등록 가능한 scope 는 PARTNER, PERSONAL_EMAIL 뿐이다.
 */
public interface RecipientDomainService {

    /** 회사의 도메인 매핑 전체 목록. */
    List<RecipientDomain> list(Long companyId);

    /**
     * 도메인 매핑 추가.
     *
     * @param scope PARTNER 또는 PERSONAL_EMAIL 만 허용
     * @throws com.shhdoc.common.ApiException scope 가 INTERNAL/EXTERNAL 이면, 또는 domain 이 중복되면
     */
    RecipientDomain create(Long companyId, String domain, RecipientScope scope);

    /**
     * 도메인 매핑 수정.
     *
     * @throws com.shhdoc.common.ApiException 대상이 없거나 다른 회사 소속이면,
     *         scope 가 INTERNAL/EXTERNAL 이면, 또는 domain 이 중복되면
     */
    RecipientDomain update(Long companyId, Long domainId, String domain, RecipientScope scope);

    /**
     * 도메인 매핑 삭제.
     *
     * @throws com.shhdoc.common.ApiException 대상이 없거나 다른 회사 소속이면
     */
    void delete(Long companyId, Long domainId);
}

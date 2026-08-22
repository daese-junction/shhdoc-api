package com.shhdoc.policy.service;

import com.shhdoc.policy.entity.Classification;
import com.shhdoc.policy.entity.PolicyAction;
import com.shhdoc.policy.entity.PolicyRule;
import com.shhdoc.policy.entity.RecipientScope;
import com.shhdoc.policy.entity.SendDirection;
import java.util.List;

/**
 * 반출 정책 규칙 관리.
 * 모든 메서드는 companyId 기준으로 테넌트를 격리한다.
 * 규칙의 조건 필드는 전부 선택이며, 비운 조건은 "무관"으로 취급된다.
 */
public interface PolicyRuleService {

    /**
     * 규칙 생성·수정 입력값. 조건 id·enum 필드는 null 허용이며 null 은 "조건 없음"을 뜻한다.
     *
     * @param name            규칙명
     * @param categoryId      대상 대분류 조건
     * @param documentTypeId  대상 문서 유형 조건
     * @param sensitiveTypeId 대상 민감정보 조건
     * @param classification  보안등급 조건
     * @param direction       발송 방향(ALL/INTERNAL/OUTBOUND). 필수.
     * @param recipientScope  수신 범위 조건. direction=OUTBOUND 일 때만 지정 가능(사외 범위 세분화).
     * @param action          매치 시 판정(ALLOW/REVIEW/BLOCK). 필수.
     */
    record RuleData(
            String name,
            Long categoryId,
            Long documentTypeId,
            Long sensitiveTypeId,
            Classification classification,
            SendDirection direction,
            RecipientScope recipientScope,
            PolicyAction action
    ) {
    }

    /** 회사의 규칙 전체 목록. */
    List<PolicyRule> list(Long companyId);

    /**
     * 규칙 추가. 생성 직후 enabled=true.
     *
     * @throws com.shhdoc.common.ApiException 조건으로 지정한 대분류·문서 유형·민감정보가
     *         없거나 다른 회사 소속이면
     */
    PolicyRule create(Long companyId, RuleData data);

    /**
     * 규칙 수정. 조건 전체를 data 값으로 덮어쓴다(부분 수정 아님).
     *
     * @throws com.shhdoc.common.ApiException 대상이 없거나 다른 회사 소속이면,
     *         또는 조건 참조 대상이 유효하지 않으면
     */
    PolicyRule update(Long companyId, Long ruleId, RuleData data);

    /**
     * 규칙 사용/중지 토글.
     *
     * @throws com.shhdoc.common.ApiException 대상이 없거나 다른 회사 소속이면
     */
    PolicyRule changeEnabled(Long companyId, Long ruleId, boolean enabled);

    /**
     * 규칙 삭제.
     *
     * @throws com.shhdoc.common.ApiException 대상이 없거나 다른 회사 소속이면
     */
    void delete(Long companyId, Long ruleId);
}

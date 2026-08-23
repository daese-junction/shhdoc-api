package com.shhdoc.upstage.policy;

import com.shhdoc.upstage.dto.ScanStatus;

/**
 * 정책 룰 하나. 조건 4개가 전부 매칭되면 {@code decision}을 적용한다. 조건은 각각
 * {@code null}이면 무관(전체 매칭) — {@code category}만 {@code null}이 아니라
 * 4개 조건 전부 이 규칙을 따른다.
 *
 * @param ruleId         출처가 된 {@code policy_rules} 행의 id. 판정 로그에 남겨
 *                       "어느 룰이 이 결정을 내렸나"를 바로 찾을 수 있게 한다.
 *                       DB 룰 하나가 여러 개로 펼쳐지므로 값이 겹칠 수 있다
 * @param category       대상 문서 카테고리(회사별 문서유형 코드). {@code null}이면 무관
 * @param recipientType  수신자 유형 — "internal"/"partner"/"personal_email"/"external".
 *                       {@code null}이면 무관
 * @param sensitiveType  대상 민감정보 유형 코드. 문서에서 검출된 유형 목록에 이 값이
 *                       하나라도 포함되면 매칭(단일값 대 목록 포함여부). {@code null}이면 무관
 * @param classification 대상 보안등급 — "PUBLIC"/"INTERNAL"/"CONFIDENTIAL"/"SECRET".
 *                       문서의 등급과 정확히 같아야 매칭. {@code null}이면 무관
 * @param decision        매칭 시 적용할 판정
 */
public record Rule(
        Long ruleId,
        String category,
        String recipientType,
        String sensitiveType,
        String classification,
        ScanStatus decision
) {

    /** 출처 id 가 필요 없을 때(주로 테스트) 쓰는 축약형. */
    public Rule(String category, String recipientType, String sensitiveType, String classification,
                ScanStatus decision) {
        this(null, category, recipientType, sensitiveType, classification, decision);
    }
}

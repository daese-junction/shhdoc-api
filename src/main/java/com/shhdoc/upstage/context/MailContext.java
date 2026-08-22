package com.shhdoc.upstage.context;

import java.util.List;

/**
 * 메일 + 문서분석결과 + 정책을 합친, 판단 직전 상태의 컨텍스트.
 * {@code decision.DecisionEngine}이 이 값을 {@code policy.Policy.rules()}에 매칭한다.
 *
 * @param senderAddress          발신자 이메일 주소
 * @param recipientAddresses     수신자 이메일 주소 목록
 * @param recipientType          수신자 유형 — "internal"/"partner"/"personal_email"/"external" 중 하나
 * @param category               문서 카테고리 (분류 결과, 회사별 문서유형 코드)
 * @param sensitiveTypeCodes     검출된 민감정보 유형 코드 목록 (회사별 등록 코드 기준)
 * @param classification         문서 보안등급 — "PUBLIC"/"INTERNAL"/"CONFIDENTIAL"/"SECRET"
 * @param confidentialityMarking 문서 내 대외비/기밀 표시 문구
 */
public record MailContext(
        String senderAddress,
        List<String> recipientAddresses,
        String recipientType,
        String category,
        List<String> sensitiveTypeCodes,
        String classification,
        String confidentialityMarking
) {
}

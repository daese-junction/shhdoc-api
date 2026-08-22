package com.shhdoc.upstage.context;

import com.shhdoc.upstage.pipeline.extract.SensitiveItem;

import java.util.List;

/**
 * 메일 + 문서분석결과 + 정책을 합친, 판단 직전 상태의 컨텍스트.
 * {@code decision.DecisionEngine}이 이 값을 {@code policy.Policy.rules()}에 매칭한다.
 *
 * @param senderAddress          발신자 이메일 주소
 * @param recipientAddresses     수신자 이메일 주소 목록
 * @param recipientType          수신자 유형. 지금은 "internal"(발신자와 모든 수신자가 같은 도메인)만
 *                                해석 가능하고, 그 외(승인된 파트너 등)는 판단할 정책 데이터가 없어서
 *                                {@code null}(미해석)로 둔다
 * @param category               문서 카테고리 (분류 결과)
 * @param sensitiveItems         감지된 민감정보 목록
 * @param containsPersonalInfo   개인정보 포함 여부
 * @param containsFinancialInfo  금액/재무정보 포함 여부
 * @param confidentialityMarking 문서 내 대외비/기밀 표시 문구
 */
public record MailContext(
        String senderAddress,
        List<String> recipientAddresses,
        String recipientType,
        String category,
        List<SensitiveItem> sensitiveItems,
        boolean containsPersonalInfo,
        boolean containsFinancialInfo,
        String confidentialityMarking
) {
}

package com.shhdoc.upstage.policy;

import com.shhdoc.upstage.dto.ScanStatus;

/**
 * 정책 룰 하나. {@code category}/{@code recipientType}이 둘 다 매칭되면 {@code decision}을 적용한다.
 *
 * @param category      대상 문서 카테고리 값 ({@code pipeline.classify.DocumentCategory#value()}와 동일 문자열).
 *                       {@code null}이면 카테고리 무관(전체 매칭)
 * @param recipientType 수신자 유형 (예: "designated-agency", "approved-partner", "other").
 *                       {@code null}이면 수신자 무관(전체 매칭)
 * @param decision       매칭 시 적용할 판정
 */
public record Rule(
        String category,
        String recipientType,
        ScanStatus decision
) {
}

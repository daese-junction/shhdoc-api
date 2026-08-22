package com.shhdoc.upstage.pipeline.extract;

import java.util.List;

/**
 * Information Extract API 응답 (문서유형 무관, 범용 민감정보 스키마 기준).
 *
 * @param sensitiveItems         감지된 민감정보 목록
 * @param containsPersonalInfo   개인정보 포함 여부
 * @param containsFinancialInfo  금액/재무정보 포함 여부
 * @param confidentialityMarking 문서 내 대외비/기밀 표시 문구 (없으면 빈 문자열)
 */
public record ExtractionResult(
        List<SensitiveItem> sensitiveItems,
        boolean containsPersonalInfo,
        boolean containsFinancialInfo,
        String confidentialityMarking
) {
}

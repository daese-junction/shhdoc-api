package com.shhdoc.upstage.pipeline.extract;

import java.util.List;

/**
 * Information Extract API 응답. 요청에 실어 보낸 {@link SensitiveInfoCategory} 후보 중
 * 실제로 검출된 코드만 담긴다.
 *
 * @param matchedSensitiveTypeCodes 검출된 민감정보 유형 코드 목록 (요청에 실어 보낸 후보 중 일부)
 * @param classification            문서 보안등급 — "PUBLIC"/"INTERNAL"/"CONFIDENTIAL"/"SECRET" 중 하나
 * @param confidentialityMarking    문서 내 대외비/기밀 표시 문구 (없으면 빈 문자열)
 */
public record ExtractionResult(
        List<String> matchedSensitiveTypeCodes,
        String classification,
        String confidentialityMarking
) {
}

package com.shhdoc.upstage.pipeline.extract;

/**
 * 추출 요청에 실어 보내는 민감정보 유형 후보. Upstage 서버에 미리 등록하는 게 아니라
 * 요청마다 이 목록을 그대로 실어 보낸다 ({@code classify.DocumentCategory}와 같은 패턴).
 *
 * @param code        회사가 등록한 민감정보 유형 코드 (예: "PERSONAL", "FINANCIAL")
 * @param description 유형을 명확히 구분짓는 설명
 */
public record SensitiveInfoCategory(
        String code,
        String description
) {
}

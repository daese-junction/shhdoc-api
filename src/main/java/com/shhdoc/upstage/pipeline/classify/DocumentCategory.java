package com.shhdoc.upstage.pipeline.classify;

/**
 * 분류 요청에 실어 보내는 카테고리 정의. Upstage 서버에 미리 등록하는 게 아니라
 * 요청마다 이 목록을 그대로 실어 보낸다.
 *
 * @param value       모델이 반환해야 하는 레이블 문자열 (예: "invoice")
 * @param description 레이블을 명확히 구분짓는 설명 (모호함 없앨수록 정확도 올라감)
 */
public record DocumentCategory(
        String value,
        String description
) {
}

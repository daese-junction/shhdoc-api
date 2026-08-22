package com.shhdoc.upstage.pipeline.classify;

/**
 * Document Classification API 응답.
 *
 * @param category        분류된 카테고리 값 (요청에 실어 보낸 {@link DocumentCategory#value} 중 하나)
 * @param confidenceScore 신뢰도 점수 (0.0 ~ 1.0)
 */
public record ClassificationResult(
        String category,
        double confidenceScore
) {
}

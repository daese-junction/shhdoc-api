package com.shhdoc.upstage.pipeline.generate;

/**
 * Generate API 응답. 판정 사유 문장 외에, 룰엔진이 {@code ALLOW}로 정한 판정이 위험해
 * 보이면 {@code REVIEW}로 올리라는 신호도 같이 받는다 (한쪽 방향뿐 — 룰엔진이 이미
 * {@code REVIEW}로 정한 건 AI가 다시 {@code ALLOW}로 낮추지 못한다).
 *
 * @param reason            판정 사유 한 문장
 * @param escalateToReview  룰엔진 판정이 {@code ALLOW}일 때만 의미 있음. true면 REVIEW로 올린다
 */
public record GenerationResult(
        String reason,
        boolean escalateToReview
) {
}

package com.shhdoc.upstage.pipeline.generate;

/**
 * Upstage Generate(Solar LLM Chat Completion) API 호출 모듈. 판정 사유 문장을 생성하고,
 * 룰엔진의 {@code ALLOW} 판정이 위험해 보이면 {@code REVIEW}로 올리라는 보조신호도 받는다.
 *
 * <p>최종 판정 자체는 항상 룰엔진(결정론적, 감사 가능)이 내린다 — AI는 사유 설명과
 * "이 ALLOW, 정말 괜찮나?" 검토만 보조한다. 룰이 이미 REVIEW로 정한 건 AI가 못 뒤집는다.
 */
public interface Generator {

    /**
     * 주어진 프롬프트를 기반으로 판정 사유와 에스컬레이션 신호를 생성합니다.
     *
     * @param prompt 시스템 지시사항 + Context + 룰엔진 판정이 결합된 프롬프트
     * @return 생성된 사유 문장과 에스컬레이션 신호
     */
    GenerationResult generate(String prompt);
}

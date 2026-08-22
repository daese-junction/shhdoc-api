package com.shhdoc.upstage.pipeline.generate;

/** Upstage Generate(Solar LLM Chat Completion) API 호출 모듈. 판단 사유 문장 등을 생성합니다. */
public interface Generator {

    /**
     * 주어진 프롬프트를 기반으로 텍스트를 생성합니다.
     *
     * @param prompt 시스템 지시사항 + Context가 결합된 프롬프트
     * @return 생성된 텍스트
     */
    String generate(String prompt);
}

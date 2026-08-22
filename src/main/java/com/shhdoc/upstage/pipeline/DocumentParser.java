package com.shhdoc.upstage.pipeline;

import com.shhdoc.upstage.dto.Attachment;

/** Upstage Document Parse API 호출 모듈. 문서 구조(텍스트/표/레이아웃)를 인식합니다. */
public interface DocumentParser {

    /**
     * 문서를 구조화된 데이터로 변환합니다.
     *
     * @param attachment 분석 대상 첨부파일
     * @return 구조화된 문서 내용 (구체 타입 미정, 추후 확정)
     */
    String parse(Attachment attachment);
}

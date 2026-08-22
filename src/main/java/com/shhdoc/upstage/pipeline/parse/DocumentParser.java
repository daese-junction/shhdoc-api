package com.shhdoc.upstage.pipeline.parse;

import com.shhdoc.upstage.pipeline.DocumentFile;

/** Upstage Document Parse API 호출 모듈. 문서 구조(텍스트/표/레이아웃)를 인식합니다. */
public interface DocumentParser {

    /**
     * 문서를 구조화된 데이터로 변환합니다.
     *
     * @param file 분석 대상 파일 (원본 바이트 포함)
     * @return 구조화된 문서 내용
     */
    ParsedDocument parse(DocumentFile file);
}

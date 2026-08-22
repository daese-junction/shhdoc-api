package com.shhdoc.upstage.document;

import com.shhdoc.upstage.pipeline.DocumentFile;

/** pipeline의 Parse/Classify/Extract를 조합 호출해 하나의 결과로 통합합니다 (UNDERSTAND 단계). */
public interface DocumentAnalyzer {

    /**
     * 첨부파일 하나를 분석합니다.
     *
     * @param file 분석 대상 파일 (원본 바이트 포함)
     * @return 통합된 분석 결과
     */
    DocumentAnalysisResult analyze(DocumentFile file);
}

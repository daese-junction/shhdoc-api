package com.shhdoc.upstage.document;

import com.shhdoc.upstage.pipeline.classify.ClassificationResult;
import com.shhdoc.upstage.pipeline.extract.ExtractionResult;
import com.shhdoc.upstage.pipeline.parse.ParsedDocument;

/**
 * 첨부파일 하나에 대한 UNDERSTAND 단계 통합 결과.
 * pipeline의 Parse/Classify/Extract 3개 결과를 그대로 묶은 것 (병렬호출 후 join).
 *
 * @param parsed         문서 구조 인식 결과
 * @param classification 문서유형 분류 결과
 * @param extraction     민감정보 추출 결과
 */
public record DocumentAnalysisResult(
        ParsedDocument parsed,
        ClassificationResult classification,
        ExtractionResult extraction
) {
}

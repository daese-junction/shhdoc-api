package com.shhdoc.upstage.pipeline.extract;

import com.shhdoc.upstage.pipeline.DocumentFile;

/**
 * Upstage Information Extract API 호출 모듈. 문서유형과 무관한 고정된 범용 민감정보
 * 스키마로 정보를 추출합니다. 문서유형 자체는 {@code classify.DocumentClassifier}가
 * 별도로 담당한다.
 */
public interface InformationExtractor {

    /**
     * 문서에서 민감정보를 추출합니다.
     *
     * @param file 추출 대상 파일
     * @return 추출 결과
     */
    ExtractionResult extract(DocumentFile file);
}

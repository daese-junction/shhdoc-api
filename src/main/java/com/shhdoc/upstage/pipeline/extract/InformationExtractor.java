package com.shhdoc.upstage.pipeline.extract;

import com.shhdoc.upstage.pipeline.DocumentFile;

import java.util.List;

/**
 * Upstage Information Extract API 호출 모듈. 문서유형은 {@code classify.DocumentClassifier}가
 * 별도로 담당하고, 여기선 민감정보 유형 검출 + 보안등급 판정만 한다.
 */
public interface InformationExtractor {

    /**
     * 문서에서 민감정보/보안등급을 추출합니다.
     *
     * @param file           추출 대상 파일
     * @param sensitiveTypes 검출 후보 민감정보 유형 목록 (요청마다 직접 실어 보냄)
     * @return 추출 결과
     */
    ExtractionResult extract(DocumentFile file, List<SensitiveInfoCategory> sensitiveTypes);
}

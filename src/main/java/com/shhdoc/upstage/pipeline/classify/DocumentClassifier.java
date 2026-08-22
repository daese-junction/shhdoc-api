package com.shhdoc.upstage.pipeline.classify;

import com.shhdoc.upstage.pipeline.DocumentFile;

import java.util.List;

/** Upstage Document Classification API 호출 모듈. 문서 종류를 분류합니다. */
public interface DocumentClassifier {

    /**
     * 문서를 주어진 카테고리 중 하나로 분류합니다.
     *
     * @param file       분류 대상 파일
     * @param categories 분류 후보 카테고리 목록 (요청마다 직접 실어 보냄, 최소 1개~최대 1000개)
     * @return 분류 결과
     */
    ClassificationResult classify(DocumentFile file, List<DocumentCategory> categories);
}

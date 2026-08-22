package com.shhdoc.upstage.pipeline.classify;

import com.shhdoc.upstage.dto.Attachment;

/** Upstage Document Classification API 호출 모듈. 문서 종류를 분류합니다. */
public interface DocumentClassifier {

    /**
     * 문서의 종류(카테고리)를 분류합니다.
     *
     * @param attachment 분류 대상 첨부파일
     * @return 분류된 문서 카테고리 (구체 타입 미정, 추후 확정)
     */
    String classify(Attachment attachment);
}

package com.shhdoc.upstage.pipeline;

import com.shhdoc.upstage.dto.Attachment;

import java.util.Map;

/** Upstage Information Extract API 호출 모듈. 문서에서 정의된 스키마의 정보를 추출합니다. */
public interface InformationExtractor {

    /**
     * 문서에서 필요한 정보를 스키마 형태로 추출합니다.
     *
     * @param attachment 추출 대상 첨부파일
     * @return 추출된 필드-값 쌍 (구체 타입 미정, 추후 확정)
     */
    Map<String, Object> extract(Attachment attachment);
}

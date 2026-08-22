package com.shhdoc.upstage.pipeline;

import com.shhdoc.upstage.dto.Attachment;

import java.util.List;

/** Upstage Document Split API 호출 모듈. 합본 파일을 논리 문서 단위로 분리합니다. */
public interface DocumentSplitter {

    /**
     * 첨부파일을 논리적인 문서 단위로 분리합니다.
     *
     * @param attachment 분리 대상 첨부파일
     * @return 분리된 문서(첨부파일 형태) 목록. 분리할 필요 없으면 원본 1건만 담아 반환.
     */
    List<Attachment> split(Attachment attachment);
}

package com.shhdoc.upstage.pipeline;

/**
 * pipeline 모듈에 전달되는 실제 파일 바이트.
 * {@code Attachment}(storageKey만 보유)와 달리 Upstage API로 바로 전송 가능한
 * 원본 바이트를 담는다. storageKey → 바이트 조회는 pipeline 밖에서 이미 끝난 상태여야 한다.
 *
 * @param fileName 파일명 (확장자로 포맷 판별용)
 * @param content  파일 원본 바이트
 */
public record DocumentFile(
        String fileName,
        byte[] content
) {
}

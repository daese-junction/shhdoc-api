package com.shhdoc.upstage.dto;

/**
 * 첨부파일 정보
 *
 * @param fileName   파일명
 * @param size       파일 크기 (byte)
 * @param storageKey 스토리지 저장 키
 * @param hash       파일 해시값
 */
public record Attachment(
        String fileName,
        Integer size,
        String storageKey,
        String hash
) {
}

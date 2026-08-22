package com.shhdoc.upstage.dto;

/**
 * 첨부파일별 스캔 결과
 *
 * @param storageKey 대상 첨부파일의 스토리지 저장 키
 * @param status     스캔 판정 결과
 * @param reason     판정 사유
 */
public record AttachmentResult(
        String storageKey,
        ScanStatus status,
        String reason
) {
}

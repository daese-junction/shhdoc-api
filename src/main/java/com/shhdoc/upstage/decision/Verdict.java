package com.shhdoc.upstage.decision;

import com.shhdoc.upstage.dto.ScanStatus;

/**
 * 첨부파일 1건에 대한 판정 결과. storageKey는 여기 없음 — 호출측(MailProcessor)이
 * 첨부파일 루프 안에서 이 결과를 storageKey와 묶어 {@code AttachmentResult}로 만든다.
 *
 * @param status 판정
 * @param reason 판정 사유 (Generator가 생성한 문장)
 */
public record Verdict(
        ScanStatus status,
        String reason
) {
}

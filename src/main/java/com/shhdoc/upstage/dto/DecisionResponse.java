package com.shhdoc.upstage.dto;

import java.util.List;

/**
 * 메일 판단 결과 응답
 *
 * @param mailId      판단 대상 메일 식별자
 * @param attachments 첨부파일별 스캔 결과
 */
public record DecisionResponse(
        Long mailId,
        List<AttachmentResult> attachments
) {
}

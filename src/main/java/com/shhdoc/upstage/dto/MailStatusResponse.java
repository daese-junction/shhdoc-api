package com.shhdoc.upstage.dto;

/**
 * 메일 큐 처리 상태 응답
 *
 * @param mailId 메일 식별자
 * @param status 큐 처리 상태
 */
public record MailStatusResponse(
        Integer mailId,
        QueueStatus status
) {
}

package com.shhdoc.upstage.dto;

import java.util.List;

/**
 * 메일 서비스 제공사로부터 전달받는 메일 데이터
 *
 * @param mailId        메일식별자
 * @param companyId     회사식별자
 * @param senderAddress 발신자 이메일 주소
 * @param senderId      발신자 식별자
 * @param subject       제목
 * @param body          본문
 * @param recipients    수신자 목록
 * @param attachments   첨부파일 목록
 */
public record MailRequest(
        Integer mailId,
        Integer companyId,
        String senderAddress,
        Integer senderId,
        String subject,
        String body,
        List<Recipient> recipients,
        List<Attachment> attachments
) {
}

package com.shhdoc.upstage.mail;

/** 새 검사 요청이 큐에 등록됐을 때 발행된다. 메일이 아니라 요청 단위다. */
public record MailReceivedEvent(long requestId) {
}

package com.shhdoc.upstage.mail;

/** 새 메일이 큐에 등록됐을 때 발행되는 이벤트. */
public record MailReceivedEvent(
        Integer mailId
) {
}

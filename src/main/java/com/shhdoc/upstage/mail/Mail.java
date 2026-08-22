package com.shhdoc.upstage.mail;

import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.QueueStatus;

/**
 * upstage 내부 큐에 보관하는 메일 상태. JPA 엔티티 아님 — 인메모리로만 유지되며
 * upstage 모듈은 영속화를 하지 않는다.
 */
public class Mail {

    private final Integer mailId;
    private final Integer companyId;
    private final MailRequest request;
    private volatile QueueStatus status;

    public Mail(MailRequest request) {
        this.mailId = request.mailId();
        this.companyId = request.companyId();
        this.request = request;
        this.status = QueueStatus.PENDING;
    }

    public Integer mailId() {
        return mailId;
    }

    public Integer companyId() {
        return companyId;
    }

    public MailRequest request() {
        return request;
    }

    public QueueStatus status() {
        return status;
    }

    public void markProcessing() {
        this.status = QueueStatus.PROCESSING;
    }

    public void markDone() {
        this.status = QueueStatus.DONE;
    }
}

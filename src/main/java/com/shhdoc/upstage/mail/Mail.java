package com.shhdoc.upstage.mail;

import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.QueueStatus;

import java.util.concurrent.atomic.AtomicReference;

/**
 * upstage 내부 큐에 보관하는 메일 상태. JPA 엔티티 아님 — 인메모리로만 유지되며
 * upstage 모듈은 영속화를 하지 않는다.
 */
public class Mail {

    private final Integer mailId;
    private final Integer companyId;
    private final MailRequest request;
    private final AtomicReference<QueueStatus> status = new AtomicReference<>(QueueStatus.PENDING);

    public Mail(MailRequest request) {
        this.mailId = request.mailId();
        this.companyId = request.companyId();
        this.request = request;
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
        return status.get();
    }

    /**
     * PENDING → PROCESSING 전이를 시도한다. 이미 처리중이거나 완료된 메일이면
     * 아무것도 안 하고 false를 리턴한다 — 같은 mailId가 동시에 두 번 처리되는 걸 막는 가드.
     *
     * @return 전이에 성공했으면 true
     */
    public boolean tryMarkProcessing() {
        return status.compareAndSet(QueueStatus.PENDING, QueueStatus.PROCESSING);
    }

    public void markDone() {
        status.set(QueueStatus.DONE);
    }
}

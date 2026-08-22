package com.shhdoc.upstage.mail;

import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.QueueStatus;
import java.util.concurrent.atomic.AtomicReference;

/**
 * upstage 내부 큐에 보관하는 메일 상태. JPA 엔티티 아님 — 인메모리로만 유지되며
 * upstage 모듈은 영속화를 하지 않는다.
 *
 * <p>큐에서의 신원은 {@code mailId}가 아니라 {@code requestId}다. 같은 메일을 여러 번
 * (첨부를 추가할 때마다, 재검사할 때마다) 요청할 수 있어서 mailId 로 구분하면 나중 요청이
 * 앞선 요청을 덮어써 검사되지 않고 사라진다.
 */
public class Mail {

    private final long requestId;
    private final MailRequest request;
    private final AtomicReference<QueueStatus> status = new AtomicReference<>(QueueStatus.PENDING);

    Mail(long requestId, MailRequest request) {
        this.requestId = requestId;
        this.request = request;
    }

    public long requestId() {
        return requestId;
    }

    public Long mailId() {
        return request.mailId();
    }

    public Long companyId() {
        return request.companyId();
    }

    public MailRequest request() {
        return request;
    }

    public QueueStatus status() {
        return status.get();
    }

    /**
     * PENDING → PROCESSING 전이를 시도한다. 이미 처리중이거나 완료된 요청이면
     * 아무것도 안 하고 false를 리턴한다 — 같은 요청이 동시에 두 번 처리되는 걸 막는 가드.
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

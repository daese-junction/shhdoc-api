package com.shhdoc.upstage.mail;

import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.QueueStatus;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** upstage 내부 메일 큐 저장소. 인메모리로만 유지 (DB 미사용). */
@Component
public class MailStore {

    private final Map<Long, Mail> mails = new ConcurrentHashMap<>();
    private final AtomicLong requestIds = new AtomicLong();

    /**
     * 요청을 큐에 넣고 그 요청의 식별자를 돌려준다. 요청마다 새 키를 발급하므로
     * 같은 메일을 여러 번 넣어도 앞선 요청이 지워지지 않는다.
     */
    public Mail save(MailRequest request) {
        Mail mail = new Mail(requestIds.incrementAndGet(), request);
        mails.put(mail.requestId(), mail);
        return mail;
    }

    public Mail get(long requestId) {
        Mail mail = mails.get(requestId);
        if (mail == null) {
            throw new NoSuchElementException("request not found: " + requestId);
        }
        return mail;
    }

    /** 아직 결과가 발행되지 않은(PENDING/PROCESSING) 요청만 조회한다. */
    public List<Mail> findIncompleteByCompany(Long companyId) {
        return mails.values().stream()
                .filter(mail -> Objects.equals(mail.companyId(), companyId))
                .filter(mail -> mail.status() != QueueStatus.DONE)
                .toList();
    }

    /** 끝난 요청은 들고 있을 이유가 없다. 안 지우면 인메모리 맵이 계속 커진다. */
    public void remove(long requestId) {
        mails.remove(requestId);
    }
}

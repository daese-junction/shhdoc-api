package com.shhdoc.upstage.mail;

import com.shhdoc.upstage.dto.QueueStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/** upstage 내부 메일 큐 저장소. 인메모리로만 유지 (DB 미사용). */
@Component
public class MailStore {

    private final Map<Integer, Mail> mails = new ConcurrentHashMap<>();

    public void save(Mail mail) {
        mails.put(mail.mailId(), mail);
    }

    public Mail get(Integer mailId) {
        Mail mail = mails.get(mailId);
        if (mail == null) {
            throw new NoSuchElementException("mail not found: " + mailId);
        }
        return mail;
    }

    /** 아직 결과가 발행되지 않은(PENDING/PROCESSING) 메일만 조회한다. */
    public List<Mail> findIncompleteByCompany(Integer companyId) {
        return mails.values().stream()
                .filter(mail -> mail.companyId().equals(companyId))
                .filter(mail -> mail.status() != QueueStatus.DONE)
                .toList();
    }
}

package com.shhdoc.upstage;

import com.shhdoc.upstage.dto.DecisionResponse;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.MailStatusResponse;
import com.shhdoc.upstage.mail.Mail;
import com.shhdoc.upstage.mail.MailReceivedEvent;
import com.shhdoc.upstage.mail.MailStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayService implements Gateway {

    private final MailStore mailStore;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void enqueue(MailRequest request) {
        Mail mail = new Mail(request);
        mailStore.save(mail);
        eventPublisher.publishEvent(new MailReceivedEvent(mail.mailId()));
    }

    @Override
    public List<MailStatusResponse> getStatus(Integer companyId) {
        return mailStore.findIncompleteByCompany(companyId).stream()
                .map(mail -> new MailStatusResponse(mail.mailId(), mail.status()))
                .toList();
    }

    @Override
    public void publishDecision(DecisionResponse response) {
        eventPublisher.publishEvent(response);
    }

    /**
     * 메일 제공사 모듈의 실제 리스너가 생기기 전까지, 이벤트가 정상 발행되는지
     * 확인하기 위한 임시 리스너. 진짜 리스너는 upstage 밖(메일 제공사 모듈)에 있어야 한다.
     */
    @EventListener
    private void onDecisionPublished(DecisionResponse response) {
        log.info("decision published: {}", response);
    }
}

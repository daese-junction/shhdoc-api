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
}

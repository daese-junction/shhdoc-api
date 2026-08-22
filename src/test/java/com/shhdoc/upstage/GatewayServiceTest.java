package com.shhdoc.upstage;

import com.shhdoc.upstage.dto.AttachmentResult;
import com.shhdoc.upstage.dto.DecisionResponse;
import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.MailStatusResponse;
import com.shhdoc.upstage.dto.QueueStatus;
import com.shhdoc.upstage.dto.ScanStatus;
import com.shhdoc.upstage.mail.Mail;
import com.shhdoc.upstage.mail.MailReceivedEvent;
import com.shhdoc.upstage.mail.MailStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock
    private MailStore mailStore;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private GatewayService gatewayService;

    @BeforeEach
    void setUp() {
        gatewayService = new GatewayService(mailStore, eventPublisher);
    }

    private MailRequest newRequest(Integer mailId, Integer companyId) {
        return new MailRequest(mailId, companyId, "a@a.com", 1, "제목", "본문", List.of(), List.of());
    }

    @Test
    void enqueue는_큐에_저장하고_MailReceivedEvent를_발행한다() {
        gatewayService.enqueue(newRequest(1, 100));

        verify(mailStore).save(org.mockito.ArgumentMatchers.any(Mail.class));
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new MailReceivedEvent(1));
    }

    @Test
    void getStatus는_미완료건만_MailStatusResponse로_변환한다() {
        Mail mail = new Mail(newRequest(1, 100));
        when(mailStore.findIncompleteByCompany(100)).thenReturn(List.of(mail));

        List<MailStatusResponse> result = gatewayService.getStatus(100);

        assertThat(result).containsExactly(new MailStatusResponse(1, QueueStatus.PENDING));
    }

    @Test
    void publishDecision은_DecisionResponse를_그대로_이벤트로_발행한다() {
        DecisionResponse response = new DecisionResponse(1, List.of(new AttachmentResult("key", ScanStatus.ALLOW, "ok")));

        gatewayService.publishDecision(response);

        verify(eventPublisher).publishEvent(response);
    }
}

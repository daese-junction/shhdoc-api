package com.shhdoc.upstage.mail;

import com.shhdoc.upstage.dto.MailRequest;
import com.shhdoc.upstage.dto.QueueStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MailTest {

    private Mail newMail() {
        MailRequest request = new MailRequest(1, 100, "a@a.com", 1, "제목", "본문", List.of(), List.of());
        return new Mail(request);
    }

    @Test
    void 생성직후엔_PENDING이다() {
        Mail mail = newMail();

        assertThat(mail.status()).isEqualTo(QueueStatus.PENDING);
    }

    @Test
    void tryMarkProcessing은_PENDING일때만_성공한다() {
        Mail mail = newMail();

        boolean first = mail.tryMarkProcessing();
        boolean second = mail.tryMarkProcessing();

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(mail.status()).isEqualTo(QueueStatus.PROCESSING);
    }

    @Test
    void markDone_이후엔_tryMarkProcessing이_실패한다() {
        Mail mail = newMail();
        mail.tryMarkProcessing();
        mail.markDone();

        boolean result = mail.tryMarkProcessing();

        assertThat(result).isFalse();
        assertThat(mail.status()).isEqualTo(QueueStatus.DONE);
    }
}

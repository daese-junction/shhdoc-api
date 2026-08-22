package com.shhdoc.upstage.mail;

import com.shhdoc.upstage.dto.MailRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailStoreTest {

    private final MailStore store = new MailStore();

    private Mail newMail(Integer mailId, Integer companyId) {
        MailRequest request = new MailRequest(mailId, companyId, "a@a.com", 1, "제목", "본문", List.of(), List.of());
        return new Mail(request);
    }

    @Test
    void save한_메일을_get으로_조회한다() {
        Mail mail = newMail(1, 100);
        store.save(mail);

        assertThat(store.get(1)).isSameAs(mail);
    }

    @Test
    void 없는_mailId를_get하면_예외() {
        assertThatThrownBy(() -> store.get(999)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void findIncompleteByCompany는_DONE된_메일은_제외하고_같은_회사만_반환한다() {
        Mail pending = newMail(1, 100);
        Mail processing = newMail(2, 100);
        processing.tryMarkProcessing();
        Mail done = newMail(3, 100);
        done.tryMarkProcessing();
        done.markDone();
        Mail otherCompany = newMail(4, 200);

        store.save(pending);
        store.save(processing);
        store.save(done);
        store.save(otherCompany);

        List<Mail> incomplete = store.findIncompleteByCompany(100);

        assertThat(incomplete).containsExactlyInAnyOrder(pending, processing);
    }

    @Test
    void remove하면_더는_get으로_조회되지_않는다() {
        Mail mail = newMail(1, 100);
        store.save(mail);

        store.remove(1);

        assertThatThrownBy(() -> store.get(1)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 없는_mailId를_remove해도_에러_안난다() {
        store.remove(999);
    }
}

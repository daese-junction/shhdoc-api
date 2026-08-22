package com.shhdoc.upstage.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shhdoc.upstage.dto.MailRequest;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class MailStoreTest {

    private final MailStore store = new MailStore();

    private static MailRequest newRequest(Long mailId, Long companyId) {
        return new MailRequest(mailId, companyId, "a@a.com", 1L, "제목", "본문", List.of(), List.of());
    }

    @Test
    void save한_요청을_requestId로_조회한다() {
        Mail mail = store.save(newRequest(1L, 100L));

        assertThat(store.get(mail.requestId())).isSameAs(mail);
    }

    @Test
    void 없는_requestId를_get하면_예외() {
        assertThatThrownBy(() -> store.get(999L)).isInstanceOf(NoSuchElementException.class);
    }

    /** 같은 메일을 두 번 넣어도 앞선 요청이 지워지지 않아야 한다. 덮어쓰면 검사가 통째로 누락된다. */
    @Test
    void 같은_메일을_두_번_넣어도_각각_남는다() {
        Mail first = store.save(newRequest(1L, 100L));
        Mail second = store.save(newRequest(1L, 100L));

        assertThat(first.requestId()).isNotEqualTo(second.requestId());
        assertThat(store.get(first.requestId())).isSameAs(first);
        assertThat(store.get(second.requestId())).isSameAs(second);
    }

    @Test
    void findIncompleteByCompany는_DONE된_요청은_제외하고_같은_회사만_반환한다() {
        Mail pending = store.save(newRequest(1L, 100L));
        Mail processing = store.save(newRequest(2L, 100L));
        processing.tryMarkProcessing();
        Mail done = store.save(newRequest(3L, 100L));
        done.tryMarkProcessing();
        done.markDone();
        store.save(newRequest(4L, 200L));

        assertThat(store.findIncompleteByCompany(100L)).containsExactlyInAnyOrder(pending, processing);
    }

    @Test
    void remove하면_큐에서_사라진다() {
        Mail mail = store.save(newRequest(1L, 100L));

        store.remove(mail.requestId());

        assertThat(store.findIncompleteByCompany(100L)).isEmpty();
    }

    @Test
    void remove하면_더는_get으로_조회되지_않는다() {
        Mail mail = store.save(newRequest(1L, 100L));

        store.remove(mail.requestId());

        assertThatThrownBy(() -> store.get(mail.requestId())).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 없는_requestId를_remove해도_에러_안난다() {
        store.remove(999L);
    }
}

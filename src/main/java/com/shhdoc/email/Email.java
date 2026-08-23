package com.shhdoc.email;

import com.shhdoc.common.ApiException;
import com.shhdoc.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "emails", indexes = @Index(name = "idx_emails_status", columnList = "status"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /** 발신 시점의 주소를 박아둔다. 계정 이메일이 바뀌어도 보낸 기록은 그대로여야 한다. */
    @Column(nullable = false)
    private String senderAddress;

    private String subject;

    @Lob
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private Instant reviewedAt;

    @Lob
    private String reviewNote;

    private Instant sentAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EmailRecipient> recipients = new ArrayList<>();

    public Email(User sender, String subject, String body) {
        this.sender = sender;
        this.senderAddress = sender.getEmail();
        this.subject = subject;
        this.body = body;
        this.status = EmailStatus.DRAFT;
    }

    /** 수정·삭제·발송은 DRAFT 에서만. 이미 보낸 메일이 다시 큐에 들어가면 안 된다. */
    public void requireDraft() {
        if (status != EmailStatus.DRAFT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "임시보관함에 있는 메일만 수정하거나 발송할 수 있습니다.");
        }
    }

    private void requireBlocked() {
        if (status != EmailStatus.BLOCKED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "승인 대기 중인 메일이 아닙니다.");
        }
    }

    public void edit(String subject, String body) {
        requireDraft();
        this.subject = subject;
        this.body = body;
    }

    public void replaceRecipients(List<EmailRecipient> newRecipients) {
        recipients.clear();
        recipients.addAll(newRecipients);
    }

    public void markSent() {
        // SMTP를 붙인다면 여기 한 곳에서 호출한다. 그 외 코드는 손댈 필요가 없다.
        this.status = EmailStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markBlocked() {
        this.status = EmailStatus.BLOCKED;
    }

    public void approve(User reviewer, String note) {
        requireBlocked();
        review(reviewer, note);
        markSent();
    }

    public void reject(User reviewer, String note) {
        requireBlocked();
        review(reviewer, note);
        this.status = EmailStatus.REJECTED;
    }

    private void review(User reviewer, String note) {
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
        this.reviewNote = note;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}

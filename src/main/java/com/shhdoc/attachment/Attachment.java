package com.shhdoc.attachment;

import com.shhdoc.email.Email;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attachments", indexes = @Index(name = "idx_attachments_hash", columnList = "content_hash"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;

    /** 사용자가 올린 원본 파일명. 저장 키로는 쓰지 않는다. */
    @Column(nullable = false)
    private String filename;

    private Long sizeBytes;

    /** 스토리지 객체 키(UUID). 파일명과 분리해 경로 조작을 막는다. */
    @Column(nullable = false, unique = true)
    private String storageKey;

    /** SHA-256. 같은 파일이면 이전 판정을 재사용한다. */
    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScanStatus scanStatus;

    @Enumerated(EnumType.STRING)
    private Verdict verdict;

    @Lob
    private String reason;

    private Instant scannedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Attachment(Email email, String filename, Long sizeBytes, String storageKey, String contentHash) {
        this.email = email;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.contentHash = contentHash;
        this.scanStatus = ScanStatus.PENDING;
    }

    /** 다시 검사하려고 판정을 지운다. 이전 사유가 남아 있으면 화면이 낡은 이유를 계속 보여준다. */
    public void resetScan() {
        this.scanStatus = ScanStatus.PENDING;
        this.verdict = null;
        this.reason = null;
        this.scannedAt = null;
    }

    public void recordVerdict(Verdict verdict, String reason) {
        this.scanStatus = ScanStatus.DONE;
        this.verdict = verdict;
        this.reason = reason;
        this.scannedAt = Instant.now();
    }

    /**
     * 검사에 실패했다. DONE 이 아니라 FAILED 다 — DONE 으로 두면 두 가지가 깨진다.
     * 해시 재사용이 이 실패를 물려받아 같은 파일이 영원히 차단되고,
     * 기동 시 보정도 DONE 은 끝난 것으로 보고 건너뛴다.
     *
     * <p>판정은 비운다. 검사를 못 한 것이지 차단된 것이 아니다.
     */
    public void recordFailure(String reason) {
        this.scanStatus = ScanStatus.FAILED;
        this.verdict = null;
        this.reason = reason;
        this.scannedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}

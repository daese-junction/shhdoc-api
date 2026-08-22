package com.shhdoc.policy.entity;

import com.shhdoc.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 반출 정책 규칙. 조건 필드는 전부 nullable 이며 null 은 "조건 없음"을 뜻한다. */
@Entity
@Table(name = "policy_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean enabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private DocumentCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensitive_type_id")
    private SensitiveInfoType sensitiveType;

    @Enumerated(EnumType.STRING)
    private Classification classification;

    /** 발송 방향 조건. OUTBOUND 는 사외 전체를 뜻하며 recipientScope 로 더 좁힐 수 있다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SendDirection direction;

    @Enumerated(EnumType.STRING)
    private RecipientScope recipientScope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyAction action;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public PolicyRule(Company company, String name, DocumentCategory category, DocumentType documentType,
                      SensitiveInfoType sensitiveType, Classification classification,
                      SendDirection direction, RecipientScope recipientScope, PolicyAction action) {
        this.company = company;
        this.name = name;
        this.enabled = true;
        this.category = category;
        this.documentType = documentType;
        this.sensitiveType = sensitiveType;
        this.classification = classification;
        this.direction = direction;
        this.recipientScope = recipientScope;
        this.action = action;
    }

    public void update(String name, DocumentCategory category, DocumentType documentType,
                       SensitiveInfoType sensitiveType, Classification classification,
                       SendDirection direction, RecipientScope recipientScope, PolicyAction action) {
        this.name = name;
        this.category = category;
        this.documentType = documentType;
        this.sensitiveType = sensitiveType;
        this.classification = classification;
        this.direction = direction;
        this.recipientScope = recipientScope;
        this.action = action;
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

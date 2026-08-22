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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수신자 도메인 → 수신 범위 매핑.
 * INTERNAL(회사 도메인)과 EXTERNAL(미등록)은 파생값이라 저장 대상이 아니다.
 */
@Entity
@Table(name = "recipient_domains",
        uniqueConstraints = @UniqueConstraint(name = "uq_recipient_domains_company_domain",
                columnNames = {"company_id", "domain"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipientDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipientScope scope;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public RecipientDomain(Company company, String domain, RecipientScope scope) {
        this.company = company;
        this.domain = domain;
        this.scope = scope;
    }

    public void update(String domain, RecipientScope scope) {
        this.domain = domain;
        this.scope = scope;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}

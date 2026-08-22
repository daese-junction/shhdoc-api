package com.shhdoc.policy.entity;

import com.shhdoc.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** 문서 대분류. 회사 생성 시 시스템 기본값이 복사되고 이후 회사 관리자가 관리한다. */
@Entity
@Table(name = "document_categories",
        uniqueConstraints = @UniqueConstraint(name = "uq_document_categories_company_code",
                columnNames = {"company_id", "code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public DocumentCategory(Company company, String code, String name) {
        this.company = company;
        this.code = code;
        this.name = name;
    }

    public void update(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}

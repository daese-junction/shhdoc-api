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

/** 민감정보 유형. description 은 AI 탐지 프롬프트에 힌트로 주입된다. */
@Entity
@Table(name = "sensitive_info_types",
        uniqueConstraints = @UniqueConstraint(name = "uq_sensitive_info_types_company_code",
                columnNames = {"company_id", "code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SensitiveInfoType {

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

    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public SensitiveInfoType(Company company, String code, String name, String description) {
        this.company = company;
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public void update(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}

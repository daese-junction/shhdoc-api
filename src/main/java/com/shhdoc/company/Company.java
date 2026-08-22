package com.shhdoc.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** 이 회사 계정은 전부 이 도메인. 사내/사외 판정의 근거라 회사당 하나로 고정한다. */
    @Column(nullable = false, unique = true)
    private String emailDomain;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Company(String name, String emailDomain) {
        this.name = name;
        this.emailDomain = emailDomain;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}

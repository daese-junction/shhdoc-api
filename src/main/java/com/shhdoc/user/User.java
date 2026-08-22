package com.shhdoc.user;

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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String name;

    /** 회사마다 부르는 이름이 달라 표시용 문자열로 둔다. 권한은 role 이 정한다. */
    private String department;

    /** 직급도 마찬가지. 사원/대리 같은 값이 회사마다 다르다. */
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** 최신 refresh 토큰. null이면 로그아웃 상태라 재발급을 거부한다. */
    @Column(length = 512)
    private String refreshToken;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public User(Company company, String email, String passwordHash, String name, Role role) {
        this.company = company;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
    }

    /** 부서·직급은 선택값이라 생성자 대신 여기서 채운다. 빈 문자열은 null 로 모은다. */
    public void updateProfile(String department, String position) {
        this.department = blankToNull(department);
        this.position = blankToNull(position);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}

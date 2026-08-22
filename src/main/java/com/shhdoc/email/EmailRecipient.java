package com.shhdoc.email;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 수신자. 사외 주소도 담으므로 users FK가 아니라 문자열 주소로 둔다. */
@Entity
@Table(name = "email_recipients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipientType type;

    @Column(nullable = false)
    private boolean isRead = false;

    EmailRecipient(Email email, String address, RecipientType type) {
        this.email = email;
        this.address = address;
        this.type = type;
    }
}

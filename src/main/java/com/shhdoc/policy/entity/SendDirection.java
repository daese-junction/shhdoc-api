package com.shhdoc.policy.entity;

/**
 * 규칙의 발송 방향 조건.
 * ALL 은 방향 무관, INTERNAL 은 사내 수신자, OUTBOUND 는 사외 전체(PARTNER/PERSONAL_EMAIL/EXTERNAL).
 * OUTBOUND 규칙은 recipientScope 로 사외 범위를 더 좁힐 수 있다.
 */
public enum SendDirection {
    ALL, INTERNAL, OUTBOUND
}

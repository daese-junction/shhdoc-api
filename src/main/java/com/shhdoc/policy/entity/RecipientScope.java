package com.shhdoc.policy.entity;

/** 수신자 범위. INTERNAL/EXTERNAL 은 파생값이라 recipient_domains 에는 저장하지 않는다. */
public enum RecipientScope {
    INTERNAL, PARTNER, PERSONAL_EMAIL, EXTERNAL
}

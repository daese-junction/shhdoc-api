package com.shhdoc.policy.entity;

/** 정책 판정. attachment 의 Verdict 와는 별개이며 연동 시점에 매핑한다. */
public enum PolicyAction {
    ALLOW, REVIEW, BLOCK
}

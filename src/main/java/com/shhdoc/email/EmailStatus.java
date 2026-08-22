package com.shhdoc.email;

public enum EmailStatus {

    /** 작성 중. 수정·삭제·발송이 가능한 유일한 상태. */
    DRAFT,

    /** 사외 발송이라 관리자 승인 대기 중. */
    BLOCKED,

    /** 관리자가 거절함. review_note 에 사유가 있다. */
    REJECTED,

    /** 발송 완료. */
    SENT
}

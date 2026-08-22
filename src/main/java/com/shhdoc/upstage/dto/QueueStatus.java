package com.shhdoc.upstage.dto;

/** 메일 큐 처리 상태 (스캔 판정과 별개로, 큐 처리 진행 단계) */
public enum QueueStatus {
    /** 큐 등록됨, 처리 대기중 */
    PENDING,
    /** 처리(문서분석+정책판단) 진행중 */
    PROCESSING,
    /** 처리 완료 */
    DONE
}

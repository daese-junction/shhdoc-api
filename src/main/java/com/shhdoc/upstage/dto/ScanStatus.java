package com.shhdoc.upstage.dto;

/** 첨부파일 단위 스캔 판정 결과 */
public enum ScanStatus {
    /** 발송 허용 */
    ALLOW,
    /** 추가 확인 또는 승인 필요 */
    REVIEW,
    /**
     * 검사를 하지 못했다. REVIEW 와 구분해야 한다 — REVIEW 는 "봤고 사람이 확인해야 한다"이고
     * 이건 "보지 못했다"다. 뭉뚱그리면 실패한 파일이 차단 판정으로 굳어 재검사 대상에서 빠진다.
     */
    FAILED
}

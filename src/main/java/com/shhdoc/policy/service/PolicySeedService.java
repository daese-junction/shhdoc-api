package com.shhdoc.policy.service;

import com.shhdoc.company.Company;

/**
 * 회사 생성 시 시스템 기본 정책 틀을 해당 회사 데이터로 복사한다.
 * 복사 대상: 문서 대분류/유형, 민감정보 유형, 프리메일 도메인 목록, 기본 반출 규칙.
 * 복사된 데이터는 이후 회사 관리자가 자유롭게 수정·삭제한다(시스템 원본과 연결 없음).
 */
public interface PolicySeedService {

    /** 신규 회사에 기본값을 복사한다. 회사 생성 트랜잭션 안에서 한 번만 호출한다. */
    void seedFor(Company company);
}

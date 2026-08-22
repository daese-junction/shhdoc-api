package com.shhdoc.upstage.policy;

/** 기업별로 사전 동기화된 보안 정책을 조회합니다. */
public interface PolicyService {

    /**
     * 기업 식별자로 정책을 조회합니다.
     *
     * @param companyId 조회 대상 기업 식별자
     * @return 해당 기업의 보안 정책
     */
    Policy findByCompany(Integer companyId);
}

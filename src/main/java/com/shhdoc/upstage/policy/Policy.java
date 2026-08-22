package com.shhdoc.upstage.policy;

import java.util.List;

/**
 * 기업 하나의 보안 정책. 룰은 순서대로 평가하며, 먼저 매칭되는 룰을 적용한다
 * (뒤로 갈수록 더 일반적인/기본 룰을 두는 식으로 우선순위를 표현한다).
 *
 * @param companyId 기업 식별자
 * @param rules     정책 룰 목록
 */
public record Policy(
        Long companyId,
        List<Rule> rules
) {
}

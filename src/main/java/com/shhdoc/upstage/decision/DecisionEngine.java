package com.shhdoc.upstage.decision;

import com.shhdoc.upstage.context.MailContext;
import com.shhdoc.upstage.policy.Policy;

/**
 * {@code MailContext}를 {@code Policy.rules()}에 매칭해 ALLOW/REVIEW를 확정합니다 (DECIDE 단계).
 * 결정적 룰매칭 로직이며 LLM을 쓰지 않습니다 — 판정 확정 후에만 사유 문장 생성을 위해
 * {@code pipeline.generate.Generator}를 호출합니다.
 */
public interface DecisionEngine {

    /**
     * 첨부파일 1건에 대한 판정을 확정합니다.
     *
     * @param context 판단용 컨텍스트
     * @param policy  적용할 기업 정책
     * @return 판정 결과
     */
    Verdict decide(MailContext context, Policy policy);
}

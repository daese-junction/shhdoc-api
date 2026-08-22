package com.shhdoc.upstage.decision;

import com.shhdoc.upstage.context.MailContext;
import com.shhdoc.upstage.dto.ScanStatus;
import com.shhdoc.upstage.pipeline.generate.Generator;
import com.shhdoc.upstage.policy.Policy;
import com.shhdoc.upstage.policy.Rule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DecisionEngineImpl implements DecisionEngine {

    private final Generator generator;

    @Override
    public Verdict decide(MailContext context, Policy policy) {
        ScanStatus status = policy.rules().stream()
                .filter(rule -> matches(rule, context))
                .findFirst()
                .map(Rule::decision)
                .orElse(ScanStatus.REVIEW);

        String reason = generator.generate(buildReasonPrompt(context, status));
        return new Verdict(status, reason);
    }

    /**
     * recipientType 매칭은 아직 못 함 — 수신자 도메인을 유형(승인파트너 등)으로 해석할
     * 정책 데이터가 없어서, recipientType이 지정된 룰은 전부 매칭에서 제외하고 와일드카드
     * (recipientType == null) 룰만 매칭한다. STEP1 정책동기화가 생기면 여기 보강 필요.
     */
    private boolean matches(Rule rule, MailContext context) {
        boolean categoryMatches = rule.category() == null || rule.category().equals(context.category());
        boolean recipientTypeMatches = rule.recipientType() == null;
        return categoryMatches && recipientTypeMatches;
    }

    private String buildReasonPrompt(MailContext context, ScanStatus status) {
        String marking = context.confidentialityMarking() == null || context.confidentialityMarking().isBlank()
                ? "없음"
                : context.confidentialityMarking();

        return """
                다음 근거로 이메일 발송에 대한 판정 사유를 한 문장으로 작성해줘.
                - 문서유형: %s
                - 개인정보 포함: %s
                - 재무정보 포함: %s
                - 대외비 표시: %s
                - 판정: %s
                """.formatted(context.category(), context.containsPersonalInfo(),
                context.containsFinancialInfo(), marking, status);
    }
}

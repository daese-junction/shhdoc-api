package com.shhdoc.upstage.decision;

import com.shhdoc.upstage.context.MailContext;
import com.shhdoc.upstage.dto.ScanStatus;
import com.shhdoc.upstage.pipeline.generate.Generator;
import com.shhdoc.upstage.policy.Policy;
import com.shhdoc.upstage.policy.Rule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
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
                // 아무 룰도 안 걸리면 통과다. 정책은 "막을 것"만 나열하는 모델이라
                // (등록된 룰이 전부 BLOCK/REVIEW) 여기서 REVIEW 를 주면 룰과 무관한 문서까지
                // 전부 보류로 떨어진다 — 명함 한 장도 승인을 받아야 했다.
                // 사외 발송은 이 판정과 별개로 EmailService.send 가 항상 막는다.
                .orElse(ScanStatus.ALLOW);

        String reason = generator.generate(buildReasonPrompt(context, status));
        log.info("[DECISION] category={} recipientType={} sensitiveTypes={} classification={} -> status={}",
                context.category(), context.recipientType(), context.sensitiveTypeCodes(),
                context.classification(), status);
        return new Verdict(status, reason);
    }

    /** 조건 4개(문서유형/수신자유형/민감정보유형/보안등급) 전부 매칭돼야 룰이 매칭된다. 각 조건은 null이면 무관. */
    private boolean matches(Rule rule, MailContext context) {
        boolean categoryMatches = rule.category() == null || rule.category().equals(context.category());
        boolean recipientTypeMatches = rule.recipientType() == null
                || rule.recipientType().equals(context.recipientType());
        boolean sensitiveTypeMatches = rule.sensitiveType() == null
                || context.sensitiveTypeCodes().contains(rule.sensitiveType());
        boolean classificationMatches = rule.classification() == null
                || rule.classification().equals(context.classification());
        return categoryMatches && recipientTypeMatches && sensitiveTypeMatches && classificationMatches;
    }

    private String buildReasonPrompt(MailContext context, ScanStatus status) {
        String marking = context.confidentialityMarking() == null || context.confidentialityMarking().isBlank()
                ? "없음"
                : context.confidentialityMarking();

        String recipientType = context.recipientType() == null ? "외부(미분류)" : context.recipientType();
        String sensitiveTypes = context.sensitiveTypeCodes().isEmpty()
                ? "없음"
                : String.join(", ", context.sensitiveTypeCodes());
        String classification = context.classification() == null ? "미분류" : context.classification();

        return """
                다음 근거로 이메일 발송에 대한 판정 사유를 한 문장으로 작성해줘.
                - 문서유형: %s
                - 수신자 유형: %s
                - 검출된 민감정보 유형: %s
                - 보안등급: %s
                - 대외비 표시: %s
                - 판정: %s
                """.formatted(context.category(), recipientType, sensitiveTypes, classification, marking, status);
    }
}

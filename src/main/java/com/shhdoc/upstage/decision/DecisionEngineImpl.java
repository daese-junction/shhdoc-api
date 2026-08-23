package com.shhdoc.upstage.decision;

import com.shhdoc.upstage.context.MailContext;
import com.shhdoc.upstage.dto.ScanStatus;
import com.shhdoc.upstage.pipeline.generate.Generator;
import com.shhdoc.upstage.policy.Policy;
import com.shhdoc.upstage.policy.Rule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionEngineImpl implements DecisionEngine {

    private final Generator generator;

    @Override
    public Verdict decide(MailContext context, Policy policy) {
        List<Rule> matched = policy.rules().stream().filter(rule -> matches(rule, context)).toList();

        // 조건을 더 많이 건 룰이 이긴다. 먼저 걸린 룰이 이기게 두면 조건이 전부 비어 있는
        // ALLOW 룰 하나가 뒤에 있는 제한 룰을 전부 가린다 — 우선순위 컬럼이 없어 평가 순서가
        // id 순인데, 그런 룰이 먼저 만들어졌다는 이유만으로 급여대장이 사외로 나갔다.
        //
        // 엄격한 쪽을 무조건 이기게 하지 않는 이유는, "payslip 은 REVIEW, 단 사내면 ALLOW"
        // 같은 좁은 예외가 넓은 기본 룰에 묻히면 안 되기 때문이다. 조건 수가 같을 때만
        // 엄격한 쪽으로 기운다 — 우열을 못 가리면 안전한 쪽이다.
        Optional<Rule> decided = matched.stream().max(PRECEDENCE);

        // 아무 룰도 안 걸리면 통과다. 정책은 "막을 것"만 나열하는 모델이라 여기서 REVIEW 를
        // 주면 룰과 무관한 문서까지 전부 보류로 떨어진다 — 명함 한 장도 승인을 받아야 했다.
        ScanStatus status = decided.map(Rule::decision).orElse(ScanStatus.ALLOW);

        logBasis(context, policy, matched, decided, status);
        String reason = generator.generate(buildReasonPrompt(context, status));
        return new Verdict(status, reason);
    }

    /** 조건을 많이 건 룰이 이기고, 조건 수가 같으면 엄격한 쪽이 이긴다. */
    private static final Comparator<Rule> PRECEDENCE =
            Comparator.comparingInt(DecisionEngineImpl::specificity)
                    .thenComparingInt(rule -> severity(rule.decision()));

    /** 걸어둔 조건의 개수. 비워둔 조건은 "무관"이라 그만큼 넓은 룰이다. */
    private static int specificity(Rule rule) {
        return (rule.category() == null ? 0 : 1)
                + (rule.recipientType() == null ? 0 : 1)
                + (rule.sensitiveType() == null ? 0 : 1)
                + (rule.classification() == null ? 0 : 1);
    }

    /** 높을수록 엄격하다. 룰이 만들어내는 판정은 ALLOW/REVIEW 둘뿐이다 (FAILED 는 검사 실패용). */
    private static int severity(ScanStatus status) {
        return status == ScanStatus.ALLOW ? 0 : 1;
    }

    /**
     * 판정 근거를 남긴다. 문서에서 뽑은 값과 "어느 룰이 이 결정을 내렸는지"가 같이 있어야
     * 통과가 이상할 때 고칠 룰을 바로 찾을 수 있다.
     *
     * <p>안 걸린 룰이 왜 안 걸렸는지는 DEBUG 로 따로 남긴다 — 룰이 많으면 INFO 를 덮는다.
     * 필요할 때 {@code logging.level.com.shhdoc.upstage.decision=DEBUG} 로 켠다.
     */
    private void logBasis(MailContext context, Policy policy, List<Rule> matched, Optional<Rule> decided,
                          ScanStatus status) {
        log.info("[DECISION] category={} recipientType={} sensitiveTypes={} classification={} -> status={} | {}",
                context.category(), context.recipientType(), context.sensitiveTypeCodes(),
                context.classification(), status,
                decided.map(rule -> "룰 %d건 중 %d건 매칭%s, 판정한 룰=#%d %s".formatted(
                                policy.rules().size(), matched.size(),
                                matched.stream().map(Rule::ruleId).distinct().toList(),
                                rule.ruleId(), describe(rule)))
                        .orElseGet(() -> "룰 %d건 중 매칭 없음 → 기본 판정".formatted(policy.rules().size())));

        if (!log.isDebugEnabled()) {
            return;
        }
        policy.rules().stream()
                .filter(rule -> !matches(rule, context))
                .forEach(rule -> log.debug("[DECISION] 룰 #{} 미매칭 — {} / {}",
                        rule.ruleId(), mismatch(rule, context), describe(rule)));
    }

    private static String describe(Rule rule) {
        return "(category=%s recipientType=%s sensitiveType=%s classification=%s -> %s)".formatted(
                rule.category(), rule.recipientType(), rule.sensitiveType(), rule.classification(), rule.decision());
    }

    /** 어긋난 첫 조건 하나만 말한다. 전부 나열하면 정작 원인이 묻힌다. */
    private static String mismatch(Rule rule, MailContext context) {
        if (!categoryMatches(rule, context)) {
            return "category 불일치 (룰=%s, 문서=%s)".formatted(rule.category(), context.category());
        }
        if (!recipientTypeMatches(rule, context)) {
            return "recipientType 불일치 (룰=%s, 수신자=%s)".formatted(rule.recipientType(), context.recipientType());
        }
        if (!sensitiveTypeMatches(rule, context)) {
            return "sensitiveType 미검출 (룰=%s, 검출=%s)".formatted(rule.sensitiveType(), context.sensitiveTypeCodes());
        }
        return "classification 불일치 (룰=%s, 문서=%s)".formatted(rule.classification(), context.classification());
    }

    /** 조건 4개(문서유형/수신자유형/민감정보유형/보안등급) 전부 매칭돼야 룰이 매칭된다. 각 조건은 null이면 무관. */
    private boolean matches(Rule rule, MailContext context) {
        return categoryMatches(rule, context)
                && recipientTypeMatches(rule, context)
                && sensitiveTypeMatches(rule, context)
                && classificationMatches(rule, context);
    }

    private static boolean categoryMatches(Rule rule, MailContext context) {
        return rule.category() == null || rule.category().equals(context.category());
    }

    private static boolean recipientTypeMatches(Rule rule, MailContext context) {
        return rule.recipientType() == null || rule.recipientType().equals(context.recipientType());
    }

    private static boolean sensitiveTypeMatches(Rule rule, MailContext context) {
        return rule.sensitiveType() == null || context.sensitiveTypeCodes().contains(rule.sensitiveType());
    }

    private static boolean classificationMatches(Rule rule, MailContext context) {
        return rule.classification() == null || rule.classification().equals(context.classification());
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

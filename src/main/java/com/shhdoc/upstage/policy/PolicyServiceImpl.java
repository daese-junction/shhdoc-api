package com.shhdoc.upstage.policy;

import com.shhdoc.policy.entity.Classification;
import com.shhdoc.policy.entity.PolicyAction;
import com.shhdoc.policy.entity.PolicyRule;
import com.shhdoc.policy.entity.RecipientScope;
import com.shhdoc.policy.entity.SendDirection;
import com.shhdoc.policy.repository.PolicyRuleRepository;
import com.shhdoc.upstage.dto.ScanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 회사가 실제 등록한 정책({@code com.shhdoc.policy})을 조회해 upstage 판정 모델로 변환한다.
 * 조건 4개(문서유형/수신범위/민감정보유형/보안등급) 전부 실제 데이터로 매칭된다.
 */
@Component
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private static final List<String> OUTBOUND_ANY_SCOPES = List.of("partner", "personal_email", "external");

    private final PolicyRuleRepository ruleRepository;

    @Override
    public Policy findByCompany(Long companyId) {
        List<Rule> rules = ruleRepository.findByCompanyIdOrderByIdAsc(companyId).stream()
                .filter(PolicyRule::isEnabled)
                .flatMap(rule -> toRules(rule).stream())
                .toList();
        return new Policy(companyId, rules);
    }

    /**
     * {@code PolicyRule} 하나가 upstage {@code Rule} 여러 개로 펼쳐질 수 있다 —
     * {@code direction=OUTBOUND}인데 {@code recipientScope}를 안 정한 룰은 "사외 전체"
     * (PARTNER/PERSONAL_EMAIL/EXTERNAL 전부)를 뜻하는데, upstage {@code Rule}은 값
     * 하나만 정확매칭하거나 null(무관)만 지원해서 "이 중 아무거나"를 한 줄로 못 담는다.
     * 그래서 이 경우만 조건 동일한 룰 3개로 펼쳐서 매칭 시 OR처럼 동작하게 한다.
     */
    private static List<Rule> toRules(PolicyRule rule) {
        String category = rule.getDocumentType() == null ? null : rule.getDocumentType().getCode();
        String sensitiveType = rule.getSensitiveType() == null ? null : rule.getSensitiveType().getCode();
        String classification = toClassification(rule.getClassification());
        ScanStatus decision = toScanStatus(rule.getAction());

        if (rule.getDirection() == SendDirection.OUTBOUND && rule.getRecipientScope() == null) {
            return OUTBOUND_ANY_SCOPES.stream()
                    .map(recipientType -> new Rule(category, recipientType, sensitiveType, classification, decision))
                    .toList();
        }

        String recipientType = toRecipientType(rule.getDirection(), rule.getRecipientScope());
        return List.of(new Rule(category, recipientType, sensitiveType, classification, decision));
    }

    /** ALL(방향 무관)→null, INTERNAL→"internal", OUTBOUND+scope→scope 이름(소문자). */
    private static String toRecipientType(SendDirection direction, RecipientScope scope) {
        return switch (direction) {
            case ALL -> null;
            case INTERNAL -> "internal";
            case OUTBOUND -> scope.name().toLowerCase();
        };
    }

    /** upstage 쪽 classification 표현은 Extract 응답과 동일하게 대문자 이름 문자열을 쓴다. */
    private static String toClassification(Classification classification) {
        return classification == null ? null : classification.name();
    }

    /** ScanStatus엔 BLOCK이 없어서, 안전한 쪽인 REVIEW로 흡수한다. */
    private static ScanStatus toScanStatus(PolicyAction action) {
        return action == PolicyAction.ALLOW ? ScanStatus.ALLOW : ScanStatus.REVIEW;
    }
}

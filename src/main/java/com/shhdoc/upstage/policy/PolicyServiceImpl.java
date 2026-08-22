package com.shhdoc.upstage.policy;

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
 *
 * <p><b>Stage A(지금)</b>: upstage 모델(category/recipientType 2개 조건)이 실제 스키마
 * (대분류/문서유형/민감정보유형/보안등급/발송방향+수신범위, 5개 조건)보다 단순해서 아래는
 * 아직 정확히 반영 못 한다.
 * <ul>
 *   <li>문서유형(category) 조건 — {@code DocumentType.code}(예: "PAYROLL")와
 *       Classification 결과값(예: "payslip")의 어휘가 달라서, documentType이 지정된
 *       룰은 실질적으로 매칭되지 않는다.</li>
 *   <li>recipientScope의 PARTNER/PERSONAL_EMAIL 조건 — {@code ContextBuilderImpl}이
 *       아직 회사별 등록 도메인을 조회하지 않아서 internal/external 둘로만 해석하므로,
 *       PARTNER/PERSONAL_EMAIL이 지정된 룰도 매칭되지 않는다.</li>
 * </ul>
 * Stage B에서 Classification을 회사별 문서유형 기준으로 바꾸고, recipientType 해석을
 * {@code RecipientDomain} 조회로 보강하면 해소된다.
 */
@Component
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRuleRepository ruleRepository;

    @Override
    public Policy findByCompany(Long companyId) {
        List<Rule> rules = ruleRepository.findByCompanyIdOrderByIdAsc(companyId).stream()
                .filter(PolicyRule::isEnabled)
                .map(PolicyServiceImpl::toRule)
                .toList();
        return new Policy(companyId, rules);
    }

    private static Rule toRule(PolicyRule rule) {
        String category = rule.getDocumentType() == null ? null : rule.getDocumentType().getCode();
        String recipientType = toRecipientType(rule.getDirection(), rule.getRecipientScope());
        ScanStatus decision = toScanStatus(rule.getAction());
        return new Rule(category, recipientType, decision);
    }

    /**
     * ALL(방향 무관)→null, INTERNAL→"internal", OUTBOUND(+scope)→scope 이름(소문자)
     * 또는 scope 미지정이면 "external".
     */
    private static String toRecipientType(SendDirection direction, RecipientScope scope) {
        return switch (direction) {
            case ALL -> null;
            case INTERNAL -> "internal";
            case OUTBOUND -> scope == null ? "external" : scope.name().toLowerCase();
        };
    }

    /** ScanStatus엔 BLOCK이 없어서, 안전한 쪽인 REVIEW로 흡수한다. */
    private static ScanStatus toScanStatus(PolicyAction action) {
        return action == PolicyAction.ALLOW ? ScanStatus.ALLOW : ScanStatus.REVIEW;
    }
}

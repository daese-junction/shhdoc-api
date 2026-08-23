package com.shhdoc.upstage.policy;

import com.shhdoc.policy.entity.Classification;
import com.shhdoc.policy.entity.DocumentType;
import com.shhdoc.policy.entity.PolicyAction;
import com.shhdoc.policy.entity.PolicyRule;
import com.shhdoc.policy.entity.RecipientScope;
import com.shhdoc.policy.entity.SendDirection;
import com.shhdoc.policy.repository.DocumentTypeRepository;
import com.shhdoc.policy.repository.PolicyRuleRepository;
import com.shhdoc.upstage.dto.ScanStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 회사가 실제 등록한 정책({@code com.shhdoc.policy})을 조회해 upstage 판정 모델로 변환한다.
 * 조건 5개(대분류/문서유형/수신범위/민감정보유형/보안등급) 전부 실제 데이터로 매칭된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private static final List<String> OUTBOUND_ANY_SCOPES = List.of("partner", "personal_email", "external");

    private final PolicyRuleRepository ruleRepository;
    private final DocumentTypeRepository documentTypeRepository;

    /**
     * 트랜잭션을 연다. 호출자({@code MailProcessor.handle})가 {@code @Async} 워커에서
     * 돌아 세션이 없고, {@code open-in-view: false} 라 열려 있지도 않다. 그냥 조회하면
     * {@code toRules} 에서 지연 프록시(sensitiveType/documentType)를 건드리다 터진다.
     */
    @Override
    @Transactional(readOnly = true)
    public Policy findByCompany(Long companyId) {
        Map<Long, List<String>> documentTypeCodesByCategoryId = documentTypeRepository
                .findByCompanyIdOrderByIdAsc(companyId).stream()
                .collect(Collectors.groupingBy(dt -> dt.getCategory().getId(),
                        Collectors.mapping(DocumentType::getCode, Collectors.toList())));

        List<Rule> rules = ruleRepository.findByCompanyIdOrderByIdAsc(companyId).stream()
                .filter(PolicyRule::isEnabled)
                .flatMap(rule -> toRules(rule, documentTypeCodesByCategoryId).stream())
                .toList();
        log.info("[POLICY] companyId={} 룰 {}건 로드", companyId, rules.size());
        return new Policy(companyId, rules);
    }

    /**
     * {@code PolicyRule} 하나가 upstage {@code Rule} 여러 개로 펼쳐질 수 있다 — 조건 두 개가
     * "이 중 아무거나"를 뜻하는데 upstage {@code Rule}은 값 하나만 정확매칭하거나 null(무관)만
     * 지원해서 한 줄로 못 담기 때문이다.
     * <ul>
     *   <li>{@code category}(대분류)만 있고 {@code documentType}(세부유형)이 없으면 —
     *       그 대분류에 속한 문서유형 코드 전부</li>
     *   <li>{@code direction=OUTBOUND}인데 {@code recipientScope}가 없으면 — 사외 전체
     *       (PARTNER/PERSONAL_EMAIL/EXTERNAL)</li>
     * </ul>
     * 두 축의 카티션 곱으로 펼친다.
     */
    private static List<Rule> toRules(PolicyRule rule, Map<Long, List<String>> documentTypeCodesByCategoryId) {
        List<String> categories = resolveCategories(rule, documentTypeCodesByCategoryId);
        List<String> recipientTypes = resolveRecipientTypes(rule);
        String sensitiveType = rule.getSensitiveType() == null ? null : rule.getSensitiveType().getCode();
        String classification = toClassification(rule.getClassification());
        ScanStatus decision = toScanStatus(rule.getAction());

        return categories.stream()
                .flatMap(category -> recipientTypes.stream()
                        .map(recipientType -> new Rule(rule.getId(), category, recipientType, sensitiveType,
                                classification, decision)))
                .toList();
    }

    /**
     * 세부유형이 지정돼있으면 그 코드 하나. 대분류만 있으면 그 대분류에 속한 문서유형 코드 전부
     * (회사가 그 대분류에 문서유형을 하나도 안 만들었으면 매칭될 문서가 없다는 뜻이라 빈 목록 —
     * 룰 자체가 사라진다). 둘 다 없으면 무관(단일 {@code null}).
     */
    private static List<String> resolveCategories(PolicyRule rule, Map<Long, List<String>> documentTypeCodesByCategoryId) {
        if (rule.getDocumentType() != null) {
            return List.of(rule.getDocumentType().getCode());
        }
        if (rule.getCategory() != null) {
            return documentTypeCodesByCategoryId.getOrDefault(rule.getCategory().getId(), List.of());
        }
        return Collections.singletonList(null);
    }

    private static List<String> resolveRecipientTypes(PolicyRule rule) {
        if (rule.getDirection() == SendDirection.OUTBOUND && rule.getRecipientScope() == null) {
            return OUTBOUND_ANY_SCOPES;
        }
        return Collections.singletonList(toRecipientType(rule.getDirection(), rule.getRecipientScope()));
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

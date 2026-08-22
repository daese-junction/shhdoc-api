package com.shhdoc.upstage.policy;

import com.shhdoc.policy.entity.Classification;
import com.shhdoc.policy.entity.DocumentType;
import com.shhdoc.policy.entity.PolicyAction;
import com.shhdoc.policy.entity.PolicyRule;
import com.shhdoc.policy.entity.RecipientScope;
import com.shhdoc.policy.entity.SendDirection;
import com.shhdoc.policy.entity.SensitiveInfoType;
import com.shhdoc.policy.repository.PolicyRuleRepository;
import com.shhdoc.upstage.dto.ScanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceImplTest {

    @Mock
    private PolicyRuleRepository ruleRepository;

    private PolicyServiceImpl policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyServiceImpl(ruleRepository);
    }

    private PolicyRule ruleMock(boolean enabled, DocumentType documentType, SendDirection direction,
                                 RecipientScope scope, PolicyAction action) {
        PolicyRule rule = org.mockito.Mockito.mock(PolicyRule.class);
        lenient().when(rule.isEnabled()).thenReturn(enabled);
        lenient().when(rule.getDocumentType()).thenReturn(documentType);
        lenient().when(rule.getDirection()).thenReturn(direction);
        lenient().when(rule.getRecipientScope()).thenReturn(scope);
        lenient().when(rule.getAction()).thenReturn(action);
        return rule;
    }

    @Test
    void companyId를_그대로_담아서_리턴한다() {
        when(ruleRepository.findByCompanyIdOrderByIdAsc(42L)).thenReturn(List.of());

        Policy policy = policyService.findByCompany(42L);

        assertThat(policy.companyId()).isEqualTo(42L);
    }

    @Test
    void 비활성화된_룰은_제외한다() {
        PolicyRule enabled = ruleMock(true, null, SendDirection.ALL, null, PolicyAction.ALLOW);
        PolicyRule disabled = ruleMock(false, null, SendDirection.ALL, null, PolicyAction.ALLOW);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(enabled, disabled));

        Policy policy = policyService.findByCompany(1L);

        assertThat(policy.rules()).hasSize(1);
    }

    @Test
    void direction_ALL은_recipientType이_null이다() {
        PolicyRule rule = ruleMock(true, null, SendDirection.ALL, null, PolicyAction.ALLOW);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(rule));

        Rule converted = policyService.findByCompany(1L).rules().get(0);

        assertThat(converted.recipientType()).isNull();
    }

    @Test
    void direction_INTERNAL은_internal로_변환된다() {
        PolicyRule rule = ruleMock(true, null, SendDirection.INTERNAL, null, PolicyAction.ALLOW);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(rule));

        Rule converted = policyService.findByCompany(1L).rules().get(0);

        assertThat(converted.recipientType()).isEqualTo("internal");
    }

    @Test
    void OUTBOUND_scope없으면_외부3종_룰로_펼쳐진다() {
        PolicyRule rule = ruleMock(true, null, SendDirection.OUTBOUND, null, PolicyAction.REVIEW);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(rule));

        List<Rule> converted = policyService.findByCompany(1L).rules();

        assertThat(converted).extracting(Rule::recipientType)
                .containsExactlyInAnyOrder("partner", "personal_email", "external");
        assertThat(converted).allMatch(r -> r.decision() == ScanStatus.REVIEW);
    }

    @Test
    void OUTBOUND_scope있으면_소문자로_변환된다() {
        PolicyRule rule = ruleMock(true, null, SendDirection.OUTBOUND, RecipientScope.PARTNER, PolicyAction.ALLOW);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(rule));

        Rule converted = policyService.findByCompany(1L).rules().get(0);

        assertThat(converted.recipientType()).isEqualTo("partner");
    }

    @Test
    void documentType이_있으면_그_code가_category가_된다() {
        DocumentType documentType = org.mockito.Mockito.mock(DocumentType.class);
        when(documentType.getCode()).thenReturn("PAYROLL");
        PolicyRule rule = ruleMock(true, documentType, SendDirection.ALL, null, PolicyAction.ALLOW);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(rule));

        Rule converted = policyService.findByCompany(1L).rules().get(0);

        assertThat(converted.category()).isEqualTo("PAYROLL");
    }

    @Test
    void action_ALLOW는_ScanStatus_ALLOW로_변환된다() {
        PolicyRule rule = ruleMock(true, null, SendDirection.ALL, null, PolicyAction.ALLOW);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(rule));

        Rule converted = policyService.findByCompany(1L).rules().get(0);

        assertThat(converted.decision()).isEqualTo(ScanStatus.ALLOW);
    }

    @Test
    void action_REVIEW와_BLOCK은_전부_ScanStatus_REVIEW로_변환된다() {
        PolicyRule reviewRule = ruleMock(true, null, SendDirection.ALL, null, PolicyAction.REVIEW);
        PolicyRule blockRule = ruleMock(true, null, SendDirection.ALL, null, PolicyAction.BLOCK);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(reviewRule, blockRule));

        List<Rule> converted = policyService.findByCompany(1L).rules();

        assertThat(converted).extracting(Rule::decision)
                .containsExactly(ScanStatus.REVIEW, ScanStatus.REVIEW);
    }

    @Test
    void sensitiveType이_있으면_그_code가_담긴다() {
        SensitiveInfoType sensitiveType = org.mockito.Mockito.mock(SensitiveInfoType.class);
        when(sensitiveType.getCode()).thenReturn("CREDENTIAL");
        PolicyRule rule = ruleMock(true, null, SendDirection.ALL, null, PolicyAction.ALLOW);
        lenient().when(rule.getSensitiveType()).thenReturn(sensitiveType);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(rule));

        Rule converted = policyService.findByCompany(1L).rules().get(0);

        assertThat(converted.sensitiveType()).isEqualTo("CREDENTIAL");
    }

    @Test
    void classification이_있으면_이름이_담긴다() {
        PolicyRule rule = ruleMock(true, null, SendDirection.ALL, null, PolicyAction.ALLOW);
        lenient().when(rule.getClassification()).thenReturn(Classification.SECRET);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(rule));

        Rule converted = policyService.findByCompany(1L).rules().get(0);

        assertThat(converted.classification()).isEqualTo("SECRET");
    }

    @Test
    void sensitiveType과_classification이_없으면_null이다() {
        PolicyRule rule = ruleMock(true, null, SendDirection.ALL, null, PolicyAction.ALLOW);
        when(ruleRepository.findByCompanyIdOrderByIdAsc(1L)).thenReturn(List.of(rule));

        Rule converted = policyService.findByCompany(1L).rules().get(0);

        assertThat(converted.sensitiveType()).isNull();
        assertThat(converted.classification()).isNull();
    }
}

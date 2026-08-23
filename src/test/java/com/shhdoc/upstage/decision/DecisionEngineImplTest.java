package com.shhdoc.upstage.decision;

import com.shhdoc.upstage.context.MailContext;
import com.shhdoc.upstage.dto.ScanStatus;
import com.shhdoc.upstage.pipeline.generate.GenerationResult;
import com.shhdoc.upstage.pipeline.generate.Generator;
import com.shhdoc.upstage.policy.Policy;
import com.shhdoc.upstage.policy.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionEngineImplTest {

    @Mock
    private Generator generator;

    private DecisionEngineImpl decisionEngine;

    @BeforeEach
    void setUp() {
        decisionEngine = new DecisionEngineImpl(generator);
    }

    private MailContext contextWith(String category, String recipientType) {
        return contextWith(category, recipientType, List.of(), null);
    }

    private MailContext contextWith(String category, String recipientType, List<String> sensitiveTypeCodes,
                                     String classification) {
        return new MailContext("a@a.com", List.of("b@b.com"), recipientType, category,
                sensitiveTypeCodes, classification, "");
    }

    @Test
    void recipientType_해석이_안된_외부발송은_와일드카드_룰로_REVIEW된다() {
        Policy policy = new Policy(1L, List.of(
                new Rule("payslip", "internal", null, null, ScanStatus.ALLOW),
                new Rule("payslip", "designated-agency", null, null, ScanStatus.ALLOW),
                new Rule("payslip", "approved-partner", null, null, ScanStatus.REVIEW),
                new Rule("payslip", null, null, null, ScanStatus.REVIEW),
                new Rule(null, null, null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        Verdict verdict = decisionEngine.decide(contextWith("payslip", null), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
        assertThat(verdict.reason()).isEqualTo("사유 문장");
    }

    @Test
    void 내부발송이면_internal_룰이_매칭된다() {
        Policy policy = new Policy(1L, List.of(
                new Rule("payslip", "internal", null, null, ScanStatus.ALLOW),
                new Rule("payslip", null, null, null, ScanStatus.REVIEW),
                new Rule(null, null, null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        Verdict verdict = decisionEngine.decide(contextWith("payslip", "internal"), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.ALLOW);
    }

    @Test
    void 승인파트너처럼_아직_해석못하는_유형의_룰은_매칭되지_않는다() {
        Policy policy = new Policy(1L, List.of(
                new Rule("payslip", "approved-partner", null, null, ScanStatus.ALLOW),
                new Rule("payslip", null, null, null, ScanStatus.REVIEW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        // context.recipientType()이 null(미해석)이라 "approved-partner" 룰과는 매칭 안 되고
        // 와일드카드(payslip, null) 룰로 떨어져야 한다.
        Verdict verdict = decisionEngine.decide(contextWith("payslip", null), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
    }

    @Test
    void 카테고리가_안맞으면_전체와일드카드_룰로_폴백한다() {
        Policy policy = new Policy(1L, List.of(
                new Rule("payslip", null, null, null, ScanStatus.REVIEW),
                new Rule(null, null, null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        Verdict verdict = decisionEngine.decide(contextWith("contract", null), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.ALLOW);
    }

    /**
     * 정책은 막을 것만 나열하는 모델이라 안 걸리면 통과다. REVIEW 로 두면 룰과 무관한 문서까지
     * 전부 보류로 떨어져 명함 한 장도 승인을 받아야 했다. 사외 발송은 이 판정과 별개로
     * {@code EmailService.send} 가 항상 막는다.
     */
    @Test
    void 매칭되는_룰이_없으면_ALLOW로_기본판정한다() {
        Policy policy = new Policy(1L, List.of(
                new Rule("payslip", null, null, null, ScanStatus.REVIEW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        Verdict verdict = decisionEngine.decide(contextWith("contract", null), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.ALLOW);
    }

    /**
     * 운영에서 급여대장이 사외로 나갔던 상황이다. 조건이 전부 비어 있는 ALLOW 룰(#47)이
     * 제한 룰(#48)보다 id 가 작다는 이유만으로 먼저 평가돼 전부를 가렸다.
     * 순서가 아니라 조건을 더 많이 건 쪽이 이겨야 한다.
     */
    @Test
    void 전체허용_룰이_먼저여도_제한_룰이_이긴다() {
        Policy policy = new Policy(1L, List.of(
                new Rule(47L, null, null, null, null, ScanStatus.ALLOW),
                new Rule(48L, "payslip", "external", null, null, ScanStatus.REVIEW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        Verdict verdict = decisionEngine.decide(contextWith("payslip", "external"), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
    }

    /** 제한 룰이 하나도 안 걸리면 전체허용 룰이 그대로 적용된다. */
    @Test
    void 제한_룰이_안_걸리면_전체허용_룰로_통과한다() {
        Policy policy = new Policy(1L, List.of(
                new Rule(47L, null, null, null, null, ScanStatus.ALLOW),
                new Rule(48L, "payslip", "external", null, null, ScanStatus.REVIEW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        Verdict verdict = decisionEngine.decide(contextWith("business_card", "internal"), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.ALLOW);
    }

    /** 사내 발송이 실제로 겪던 상황 — 등록된 룰이 전부 OUTBOUND 라 하나도 안 걸린다. */
    @Test
    void 사외룰만_있는_정책에서_사내발송은_통과한다() {
        Policy policy = new Policy(1L, List.of(
                new Rule(null, "partner", null, null, ScanStatus.REVIEW),
                new Rule(null, "personal_email", null, null, ScanStatus.REVIEW),
                new Rule(null, "external", null, null, ScanStatus.REVIEW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        Verdict verdict = decisionEngine.decide(contextWith("business_card", "internal"), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.ALLOW);
    }

    @Test
    void 검출된_민감정보유형_목록에_룰이_요구하는_유형이_있으면_매칭된다() {
        Policy policy = new Policy(1L, List.of(
                new Rule(null, null, "CREDENTIAL", null, ScanStatus.REVIEW),
                new Rule(null, null, null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        MailContext context = contextWith("payslip", null, List.of("PERSONAL", "CREDENTIAL"), null);
        Verdict verdict = decisionEngine.decide(context, policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
    }

    @Test
    void 검출된_민감정보유형_목록에_룰이_요구하는_유형이_없으면_매칭되지_않는다() {
        Policy policy = new Policy(1L, List.of(
                new Rule(null, null, "CREDENTIAL", null, ScanStatus.REVIEW),
                new Rule(null, null, null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        MailContext context = contextWith("payslip", null, List.of("PERSONAL"), null);
        Verdict verdict = decisionEngine.decide(context, policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.ALLOW);
    }

    @Test
    void 보안등급이_정확히_같아야_매칭된다() {
        Policy policy = new Policy(1L, List.of(
                new Rule(null, null, null, "SECRET", ScanStatus.REVIEW),
                new Rule(null, null, null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", false));

        MailContext secretContext = contextWith("payslip", null, List.of(), "SECRET");
        MailContext confidentialContext = contextWith("payslip", null, List.of(), "CONFIDENTIAL");

        assertThat(decisionEngine.decide(secretContext, policy).status()).isEqualTo(ScanStatus.REVIEW);
        assertThat(decisionEngine.decide(confidentialContext, policy).status()).isEqualTo(ScanStatus.ALLOW);
    }

    /** 룰엔진이 ALLOW로 흘려보낸 걸 AI가 위험하다고 판단하면 REVIEW로 올린다. */
    @Test
    void 룰엔진_ALLOW를_AI가_위험하다고_판단하면_REVIEW로_올린다() {
        Policy policy = new Policy(1L, List.of());
        when(generator.generate(anyString())).thenReturn(new GenerationResult("여러 민감정보가 겹쳐 위험", true));

        Verdict verdict = decisionEngine.decide(contextWith("contract", null), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
        assertThat(verdict.reason()).isEqualTo("여러 민감정보가 겹쳐 위험");
    }

    /** 룰엔진이 이미 REVIEW로 정한 건 AI가 escalate를 true로 줘도 그대로 REVIEW다 (내릴 수 없음). */
    @Test
    void 룰엔진_REVIEW는_AI_에스컬레이션과_무관하게_REVIEW로_유지된다() {
        Policy policy = new Policy(1L, List.of(
                new Rule(null, null, null, null, ScanStatus.REVIEW)
        ));
        when(generator.generate(anyString())).thenReturn(new GenerationResult("사유 문장", true));

        Verdict verdict = decisionEngine.decide(contextWith("contract", null), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
    }
}

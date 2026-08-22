package com.shhdoc.upstage.decision;

import com.shhdoc.upstage.context.MailContext;
import com.shhdoc.upstage.dto.ScanStatus;
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
        return new MailContext("a@a.com", List.of("b@b.com"), recipientType, category, List.of(), false, false, "");
    }

    @Test
    void recipientType_해석이_안된_외부발송은_와일드카드_룰로_REVIEW된다() {
        Policy policy = new Policy(1, List.of(
                new Rule("payslip", "internal", ScanStatus.ALLOW),
                new Rule("payslip", "designated-agency", ScanStatus.ALLOW),
                new Rule("payslip", "approved-partner", ScanStatus.REVIEW),
                new Rule("payslip", null, ScanStatus.REVIEW),
                new Rule(null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn("사유 문장");

        Verdict verdict = decisionEngine.decide(contextWith("payslip", null), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
        assertThat(verdict.reason()).isEqualTo("사유 문장");
    }

    @Test
    void 내부발송이면_internal_룰이_매칭된다() {
        Policy policy = new Policy(1, List.of(
                new Rule("payslip", "internal", ScanStatus.ALLOW),
                new Rule("payslip", null, ScanStatus.REVIEW),
                new Rule(null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn("사유 문장");

        Verdict verdict = decisionEngine.decide(contextWith("payslip", "internal"), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.ALLOW);
    }

    @Test
    void 승인파트너처럼_아직_해석못하는_유형의_룰은_매칭되지_않는다() {
        Policy policy = new Policy(1, List.of(
                new Rule("payslip", "approved-partner", ScanStatus.ALLOW),
                new Rule("payslip", null, ScanStatus.REVIEW)
        ));
        when(generator.generate(anyString())).thenReturn("사유 문장");

        // context.recipientType()이 null(미해석)이라 "approved-partner" 룰과는 매칭 안 되고
        // 와일드카드(payslip, null) 룰로 떨어져야 한다.
        Verdict verdict = decisionEngine.decide(contextWith("payslip", null), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
    }

    @Test
    void 카테고리가_안맞으면_전체와일드카드_룰로_폴백한다() {
        Policy policy = new Policy(1, List.of(
                new Rule("payslip", null, ScanStatus.REVIEW),
                new Rule(null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn("사유 문장");

        Verdict verdict = decisionEngine.decide(contextWith("contract", null), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.ALLOW);
    }

    @Test
    void 매칭되는_룰이_없으면_안전하게_REVIEW로_기본판정한다() {
        Policy policy = new Policy(1, List.of(
                new Rule("payslip", null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn("사유 문장");

        Verdict verdict = decisionEngine.decide(contextWith("contract", null), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
    }
}

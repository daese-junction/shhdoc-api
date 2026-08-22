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

    private MailContext contextWithCategory(String category) {
        return new MailContext("a@a.com", List.of("b@b.com"), category, List.of(), false, false, "");
    }

    @Test
    void recipientType가_지정된_룰은_매칭에서_제외되고_와일드카드_룰이_적용된다() {
        Policy policy = new Policy(1, List.of(
                new Rule("payslip", "designated-agency", ScanStatus.ALLOW),
                new Rule("payslip", "approved-partner", ScanStatus.REVIEW),
                new Rule("payslip", null, ScanStatus.REVIEW),
                new Rule(null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn("사유 문장");

        Verdict verdict = decisionEngine.decide(contextWithCategory("payslip"), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
        assertThat(verdict.reason()).isEqualTo("사유 문장");
    }

    @Test
    void 카테고리가_안맞으면_전체와일드카드_룰로_폴백한다() {
        Policy policy = new Policy(1, List.of(
                new Rule("payslip", null, ScanStatus.REVIEW),
                new Rule(null, null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn("사유 문장");

        Verdict verdict = decisionEngine.decide(contextWithCategory("contract"), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.ALLOW);
    }

    @Test
    void 매칭되는_룰이_없으면_안전하게_REVIEW로_기본판정한다() {
        Policy policy = new Policy(1, List.of(
                new Rule("payslip", null, ScanStatus.ALLOW)
        ));
        when(generator.generate(anyString())).thenReturn("사유 문장");

        Verdict verdict = decisionEngine.decide(contextWithCategory("contract"), policy);

        assertThat(verdict.status()).isEqualTo(ScanStatus.REVIEW);
    }
}

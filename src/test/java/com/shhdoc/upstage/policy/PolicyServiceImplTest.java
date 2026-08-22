package com.shhdoc.upstage.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyServiceImplTest {

    private final PolicyServiceImpl policyService = new PolicyServiceImpl();

    @Test
    void companyId를_그대로_담아서_리턴한다() {
        Policy policy = policyService.findByCompany(42);

        assertThat(policy.companyId()).isEqualTo(42);
    }

    @Test
    void 룰목록은_비어있지_않다() {
        Policy policy = policyService.findByCompany(1);

        assertThat(policy.rules()).isNotEmpty();
    }

    @Test
    void companyId가_달라도_같은_룰을_반환한다() {
        Policy a = policyService.findByCompany(1);
        Policy b = policyService.findByCompany(2);

        assertThat(a.rules()).isEqualTo(b.rules());
    }
}

package com.shhdoc.upstage.policy;

import com.shhdoc.upstage.dto.ScanStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 임시 mock 구현. 실제로는 STEP1(정책 사전동기화)로 기업이 등록한 정책을 조회해야 하는데,
 * 그 인바운드 API가 아직 Gateway에 없어서 companyId와 무관하게 고정된 예시 정책을 반환한다.
 */
@Component
public class PolicyServiceImpl implements PolicyService {

    private static final Policy MOCK_POLICY = new Policy(
            null,
            List.of(
                    new Rule("payslip", "internal", ScanStatus.ALLOW),
                    new Rule("payslip", "designated-agency", ScanStatus.ALLOW),
                    new Rule("payslip", "approved-partner", ScanStatus.REVIEW),
                    new Rule("payslip", null, ScanStatus.REVIEW),
                    new Rule(null, null, ScanStatus.ALLOW)
            )
    );

    @Override
    public Policy findByCompany(Long companyId) {
        return new Policy(companyId, MOCK_POLICY.rules());
    }
}

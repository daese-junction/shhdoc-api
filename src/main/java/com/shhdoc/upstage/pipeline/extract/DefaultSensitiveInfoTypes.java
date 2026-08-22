package com.shhdoc.upstage.pipeline.extract;

import java.util.List;

/**
 * 회사가 민감정보 유형을 하나도 등록 안 했을 때 쓰는 ShhDoc 기본값.
 * {@code PolicySeedServiceImpl}의 시드 데이터와 코드가 같아야 회사가 나중에
 * 기본값을 등록해도 어긋나지 않는다.
 */
public final class DefaultSensitiveInfoTypes {

    public static final List<SensitiveInfoCategory> ALL = List.of(
            new SensitiveInfoCategory("PERSONAL", "개인정보 - 주민등록번호, 연락처, 주소, 계좌번호 등 개인 식별 정보"),
            new SensitiveInfoCategory("FINANCIAL", "재무정보 - 매출, 원가, 손익 등 회사 재무 수치"),
            new SensitiveInfoCategory("CUSTOMER", "고객정보 - 고객사명, 담당자, 거래 조건 등 고객 관련 정보"),
            new SensitiveInfoCategory("TRADE_SECRET", "영업기밀 - 가격 전략, 영업 전략 등 경쟁상 비밀 정보"),
            new SensitiveInfoCategory("TECHNICAL", "기술정보 - 설계, 사양, 공정 등 기술 자산 정보"),
            new SensitiveInfoCategory("SOURCE_CODE", "소스코드 - 프로그램 소스코드"),
            new SensitiveInfoCategory("CREDENTIAL", "인증정보 - 비밀번호, API 키, 토큰, 인증서 등 접근 자격 정보")
    );

    private DefaultSensitiveInfoTypes() {
    }
}

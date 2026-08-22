package com.shhdoc.upstage.pipeline.classify;

import java.util.List;

/**
 * ShhDoc이 정의한 범용 문서 분류체계 (회사별 정책과 무관, 고정).
 * 서비스 예시(급여명세서 등)에서 쓰인 카테고리 기준.
 *
 * <p>이 상수의 {@code value}는 추후 {@code policy.Policy}의 카테고리별 룰 키와
 * 정확히 일치해야 하므로, 값을 바꿀 땐 정책 쪽도 같이 맞춰야 한다.
 */
public final class DefaultDocumentCategories {

    public static final List<DocumentCategory> ALL = List.of(
            new DocumentCategory("contract", "계약서 - 당사자간 법적 합의사항이 명시된 문서"),
            new DocumentCategory("quotation", "견적서 - 상품/서비스 가격을 제시하는 문서"),
            new DocumentCategory("payslip", "급여명세서 - 직원 급여 지급/공제 내역 문서"),
            new DocumentCategory("business-plan", "사업계획서 - 사업 추진 계획 및 전략 문서"),
            new DocumentCategory("financial-document", "재무자료 - 재무제표, 회계 관련 문서"),
            new DocumentCategory("hr-document", "인사자료 - 직원 정보, 인사평가 등 인사 관련 문서"),
            new DocumentCategory("general", "일반문서 - 위 카테고리에 해당하지 않는 일반 문서")
    );

    private DefaultDocumentCategories() {
    }
}

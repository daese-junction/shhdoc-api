package com.shhdoc.upstage.pipeline.extract;

/**
 * 문서에서 감지된 민감정보 하나.
 *
 * @param type  민감정보 유형 (예: 이름, 계좌번호, 주민번호, 전화번호, 이메일, 금액 등)
 * @param value 감지된 값
 */
public record SensitiveItem(
        String type,
        String value
) {
}

package com.shhdoc.common;

/**
 * 이메일 주소 해석 규칙. 가입 가능 여부(회사 도메인 검사)와 사외 발송 판정이
 * 같은 기준을 써야 하므로 여기 한 곳에만 둔다.
 */
public final class EmailAddresses {

    private EmailAddresses() {
    }

    /** 대소문자만 다른 중복을 막기 위해 저장·조회 양쪽에서 통과시킨다. */
    public static String normalize(String email) {
        return email.trim().toLowerCase();
    }

    /** "@" 뒤 도메인. 없으면 빈 문자열이라 어떤 회사 도메인과도 일치하지 않는다. */
    public static String domainOf(String email) {
        String normalized = normalize(email);
        int at = normalized.lastIndexOf('@');
        return at < 0 ? "" : normalized.substring(at + 1);
    }
}

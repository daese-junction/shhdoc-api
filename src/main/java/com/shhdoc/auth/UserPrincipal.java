package com.shhdoc.auth;

import com.shhdoc.user.Role;

/**
 * 인증된 요청의 주체. access 토큰 claim에서 그대로 복원되므로 매 요청 DB를 안 친다.
 * 컨트롤러에서 {@code @AuthenticationPrincipal UserPrincipal} 로 받는다.
 */
public record UserPrincipal(Long id, Long companyId, Role role) {
}

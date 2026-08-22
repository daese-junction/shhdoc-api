package com.shhdoc.auth;

import com.shhdoc.auth.dto.LoginRequest;
import com.shhdoc.auth.dto.MeResponse;
import com.shhdoc.auth.dto.RefreshRequest;
import com.shhdoc.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "로그인과 토큰 관리. accessToken 은 30분, refreshToken 은 7일 유효하다.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "로그인",
            description = """
                    accessToken 과 refreshToken 을 발급한다.

                    이후 요청에는 `Authorization: Bearer {accessToken}` 헤더를 붙인다.
                    로그인할 때마다 refreshToken 이 새로 발급되며 이전 것은 무효가 된다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발급 성공"),
            @ApiResponse(responseCode = "401", description = "이메일이 없거나 비밀번호가 틀림 (둘을 구분해 알려주지 않는다)",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(
            summary = "액세스 토큰 재발급",
            description = """
                    accessToken 이 만료됐을 때(401) refreshToken 으로 새 토큰 쌍을 받는다.

                    재발급되면 이전 refreshToken 은 즉시 무효가 되므로, 응답으로 받은 새 refreshToken 을 저장해야 한다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "만료됐거나 로그아웃됐거나 이미 교체된 토큰",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    서버에 저장된 refreshToken 을 지워 재발급을 막는다.

                    이미 발급된 accessToken 은 회수할 수 없어 최대 30분 남으므로, 클라이언트도 저장한 토큰을 지워야 한다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 완료"),
            @ApiResponse(responseCode = "401", description = "토큰 없음 또는 만료", content = @Content)
    })
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.id());
    }

    @Operation(
            summary = "내 정보 조회",
            description = "토큰의 주인과 소속 회사(고정 이메일 도메인 포함)를 돌려준다. 프론트에서 로그인 상태 확인용으로 쓴다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 없음 또는 만료", content = @Content)
    })
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.me(principal.id());
    }
}

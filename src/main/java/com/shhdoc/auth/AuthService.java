package com.shhdoc.auth;

import com.shhdoc.auth.dto.LoginRequest;
import com.shhdoc.auth.dto.MeResponse;
import com.shhdoc.auth.dto.RefreshRequest;
import com.shhdoc.auth.dto.TokenResponse;
import com.shhdoc.common.ApiException;
import com.shhdoc.common.EmailAddresses;
import com.shhdoc.company.dto.CompanyResponse;
import com.shhdoc.user.User;
import com.shhdoc.user.UserRepository;
import com.shhdoc.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    /** 이메일이 없는 경우와 비밀번호가 틀린 경우를 구분해주면 계정 존재 여부가 새어나간다. */
    private static final String LOGIN_FAILED = "이메일 또는 비밀번호가 올바르지 않습니다.";
    private static final String INVALID_TOKEN = "유효하지 않거나 만료된 토큰입니다.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(EmailAddresses.normalize(request.email()))
                .filter(it -> passwordEncoder.matches(request.password(), it.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, LOGIN_FAILED));
        return issueTokens(user);
    }

    /** 서명이 맞아도 DB에 저장된 최신 토큰이 아니면 거부한다 (로그아웃/재발급된 토큰 차단). */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        Long userId = jwtProvider.parseUserId(request.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN));
        if (!request.refreshToken().equals(user.getRefreshToken())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN);
        }
        return issueTokens(user);
    }

    /** access 토큰은 회수할 수 없으므로 최대 30분 남는다. refresh만 끊는다. */
    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(User::clearRefreshToken);
    }

    public MeResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN));
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                CompanyResponse.from(user.getCompany()));
    }

    private TokenResponse issueTokens(User user) {
        String refreshToken = jwtProvider.issueRefreshToken(user);
        user.updateRefreshToken(refreshToken);
        return new TokenResponse(jwtProvider.issueAccessToken(user), refreshToken, UserResponse.from(user));
    }
}

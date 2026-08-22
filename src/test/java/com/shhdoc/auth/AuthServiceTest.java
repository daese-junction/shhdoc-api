package com.shhdoc.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.shhdoc.auth.dto.LoginRequest;
import com.shhdoc.auth.dto.RefreshRequest;
import com.shhdoc.common.ApiException;
import com.shhdoc.company.Company;
import com.shhdoc.user.Role;
import com.shhdoc.user.User;
import com.shhdoc.user.UserRepository;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String LOGIN_FAILED = "이메일 또는 비밀번호가 올바르지 않습니다.";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private static User user() {
        return new User(new Company("쉿닥", "shhdoc.com"), "bob@shhdoc.com", "hashed", "bob", Role.USER);
    }

    @Test
    void 없는_이메일로_로그인하면_401이고_계정_존재를_알려주지_않는다() {
        given(userRepository.findByEmail("nobody@shhdoc.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@shhdoc.com", "password123")))
                .isInstanceOf(ApiException.class)
                .hasMessage(LOGIN_FAILED)
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 비밀번호가_틀리면_없는_이메일과_같은_메시지로_401() {
        given(userRepository.findByEmail("bob@shhdoc.com")).willReturn(Optional.of(user()));
        given(passwordEncoder.matches("wrong-password", "hashed")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bob@shhdoc.com", "wrong-password")))
                .isInstanceOf(ApiException.class)
                .hasMessage(LOGIN_FAILED);
    }

    @Test
    void 로그아웃된_refresh_토큰으로는_재발급되지_않는다() {
        User user = user();
        user.clearRefreshToken();
        given(jwtProvider.parseUserId("stale-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("stale-token")))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 이미_재발급되어_교체된_예전_토큰은_거부된다() {
        User user = user();
        user.updateRefreshToken("new-token");
        given(jwtProvider.parseUserId("old-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("old-token")))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

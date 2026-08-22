package com.shhdoc.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shhdoc.common.ApiException;
import com.shhdoc.company.dto.AddMemberRequest;
import com.shhdoc.company.dto.CreateCompanyRequest;
import com.shhdoc.user.Role;
import com.shhdoc.user.User;
import com.shhdoc.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 회사 도메인 고정 규칙이 실제로 계정 생성을 막는지 확인한다. */
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CompanyService companyService;

    @Test
    void 이미_등록된_도메인으로_회사를_만들면_409() {
        given(companyRepository.existsByEmailDomain("shhdoc.com")).willReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(
                new CreateCompanyRequest("쉿닥", "shhdoc.com", "alice@shhdoc.com", "password123", "alice", null, null)))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.CONFLICT);

        verify(companyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 대표자_이메일이_회사_도메인과_다르면_400() {
        given(companyRepository.existsByEmailDomain("shhdoc.com")).willReturn(false);
        given(companyRepository.save(org.mockito.ArgumentMatchers.any(Company.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> companyService.createCompany(
                new CreateCompanyRequest("쉿닥", "shhdoc.com", "alice@gmail.com", "password123", "alice", null, null)))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 타사_도메인_이메일로_직원을_추가하면_400() {
        given(companyRepository.findById(1L)).willReturn(Optional.of(new Company("쉿닥", "shhdoc.com")));

        assertThatThrownBy(() -> companyService.addMember(
                1L, new AddMemberRequest("bob@gmail.com", "password123", "bob", null, null)))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 대소문자만_다른_이메일도_같은_계정으로_본다() {
        given(companyRepository.findById(1L)).willReturn(Optional.of(new Company("쉿닥", "shhdoc.com")));
        given(userRepository.existsByEmail("bob@shhdoc.com")).willReturn(true);

        assertThatThrownBy(() -> companyService.addMember(
                1L, new AddMemberRequest("Bob@ShhDoc.com", "password123", "bob", null, null)))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void 회사_도메인_이메일이면_직원이_추가된다() {
        given(companyRepository.findById(1L)).willReturn(Optional.of(new Company("쉿닥", "shhdoc.com")));
        given(userRepository.existsByEmail("bob@shhdoc.com")).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("hashed");
        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = companyService.addMember(
                1L, new AddMemberRequest("bob@shhdoc.com", "password123", "bob", "영업팀", "대리"));

        assertThat(response.email()).isEqualTo("bob@shhdoc.com");
        assertThat(response.department()).isEqualTo("영업팀");
        assertThat(response.position()).isEqualTo("대리");
    }

    @Test
    void 부서와_직급은_선택값이라_비워두면_null_로_들어간다() {
        given(companyRepository.findById(1L)).willReturn(Optional.of(new Company("쉿닥", "shhdoc.com")));
        given(userRepository.existsByEmail("bob@shhdoc.com")).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("hashed");
        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // 프론트가 빈 문자열을 보내도 미입력과 같게 본다.
        var response = companyService.addMember(
                1L, new AddMemberRequest("bob@shhdoc.com", "password123", "bob", "  ", null));

        assertThat(response.department()).isNull();
        assertThat(response.position()).isNull();
    }

    @Test
    void 구성원_목록은_내_회사_사람만_나온다() {
        Company company = new Company("쉿닥", "shhdoc.com");
        given(userRepository.findByCompanyIdOrderByIdAsc(1L)).willReturn(List.of(
                new User(company, "alice@shhdoc.com", "hashed", "alice", Role.ADMIN),
                new User(company, "bob@shhdoc.com", "hashed", "bob", Role.USER)));

        var members = companyService.listMembers(1L);

        assertThat(members).extracting("email")
                .containsExactly("alice@shhdoc.com", "bob@shhdoc.com");
    }

    @Test
    void 없는_회사를_조회하면_404() {
        given(companyRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getMyCompany(99L))
                .asInstanceOf(InstanceOfAssertFactories.type(ApiException.class))
                .extracting(ApiException::getStatus)
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}

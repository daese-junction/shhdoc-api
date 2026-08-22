package com.shhdoc.company;

import com.shhdoc.common.ApiException;
import com.shhdoc.common.EmailAddresses;
import com.shhdoc.company.dto.AddMemberRequest;
import com.shhdoc.company.dto.CompanyResponse;
import com.shhdoc.company.dto.CreateCompanyRequest;
import com.shhdoc.company.dto.CreateCompanyResponse;
import com.shhdoc.policy.service.PolicySeedService;
import com.shhdoc.user.Role;
import com.shhdoc.user.User;
import com.shhdoc.user.UserRepository;
import com.shhdoc.user.dto.UserResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PolicySeedService policySeedService;

    /** 대표자의 최초 진입점. 회사와 ADMIN 계정이 함께 생긴다. */
    @Transactional
    public CreateCompanyResponse createCompany(CreateCompanyRequest request) {
        String emailDomain = normalizeDomain(request.emailDomain());
        if (companyRepository.existsByEmailDomain(emailDomain)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 등록된 회사 도메인입니다.");
        }
        Company company = companyRepository.save(new Company(request.companyName(), emailDomain));
        policySeedService.seedFor(company);
        User admin = createUser(company, request.email(), request.password(), request.name(),
                Role.ADMIN, request.department(), request.position());
        return new CreateCompanyResponse(CompanyResponse.from(company), UserResponse.from(admin));
    }

    /** 내 회사 정보. companyId는 토큰에서 오므로 남의 회사는 조회할 수 없다. */
    public CompanyResponse getMyCompany(Long companyId) {
        return CompanyResponse.from(findCompany(companyId));
    }

    /** 사내 구성원 목록. 메일 수신자를 고를 때도 쓰라고 ADMIN 전용으로 두지 않는다. */
    public List<UserResponse> listMembers(Long companyId) {
        return userRepository.findByCompanyIdOrderByIdAsc(companyId).stream()
                .map(UserResponse::from)
                .toList();
    }

    /** 관리자가 자기 회사에 직원을 추가한다. companyId는 토큰에서 오므로 남의 회사를 건드릴 수 없다. */
    @Transactional
    public UserResponse addMember(Long companyId, AddMemberRequest request) {
        return UserResponse.from(createUser(findCompany(companyId), request.email(), request.password(),
                request.name(), Role.USER, request.department(), request.position()));
    }

    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다."));
    }

    /** 계정 생성은 대표자든 직원이든 전부 여기를 지난다. 도메인 검증이 갈라지지 않도록. */
    private User createUser(Company company, String email, String rawPassword, String name, Role role,
                            String department, String position) {
        String normalizedEmail = EmailAddresses.normalize(email);
        if (!EmailAddresses.domainOf(normalizedEmail).equals(company.getEmailDomain())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "회사 도메인(@" + company.getEmailDomain() + ") 이메일만 등록할 수 있습니다.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 등록된 이메일입니다.");
        }
        User user = new User(company, normalizedEmail, passwordEncoder.encode(rawPassword), name, role);
        user.updateProfile(department, position);
        return userRepository.save(user);
    }

    /** "@Shhdoc.com " 처럼 들어와도 "shhdoc.com" 하나로 모은다. */
    private static String normalizeDomain(String emailDomain) {
        return emailDomain.trim().toLowerCase().replaceFirst("^@", "");
    }
}

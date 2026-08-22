package com.shhdoc.company;

import com.shhdoc.auth.UserPrincipal;
import com.shhdoc.company.dto.AddMemberRequest;
import com.shhdoc.company.dto.CreateCompanyRequest;
import com.shhdoc.company.dto.CreateCompanyResponse;
import com.shhdoc.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회사", description = "회사 생성과 직원 계정 관리. 공개 회원가입은 없고, 대표자가 회사를 만든 뒤 직원을 한 명씩 추가한다.")
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(
            summary = "회사 생성 (대표자 최초 가입)",
            description = """
                    회사와 대표자 계정(ADMIN)을 한 번에 만든다. 인증이 필요 없는 유일한 계정 생성 API다.

                    emailDomain 은 이 회사의 모든 계정이 쓸 고정 도메인이며, 첨부파일의 사내/사외 발송을
                    판단하는 기준이 된다. 한 도메인은 한 회사만 쓸 수 있고, 대표자 email 도 이 도메인이어야 한다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회사와 대표자 계정 생성 완료"),
            @ApiResponse(responseCode = "400", description = "대표자 이메일이 회사 도메인과 다름",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 등록된 도메인이거나 이미 가입된 이메일",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCompanyResponse create(@Valid @RequestBody CreateCompanyRequest request) {
        return companyService.createCompany(request);
    }

    @Operation(
            summary = "직원 계정 추가 (ADMIN 전용)",
            description = """
                    관리자가 자기 회사에 직원 계정을 만든다. 초기 비밀번호는 관리자가 정해서 직원에게 전달한다.

                    소속 회사는 토큰에서 읽으므로 남의 회사에는 계정을 만들 수 없다.
                    이메일 도메인이 회사 도메인과 다르면 거부된다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "직원 계정 생성 완료 (role = USER)"),
            @ApiResponse(responseCode = "400", description = "회사 도메인이 아닌 이메일", content = @Content),
            @ApiResponse(responseCode = "401", description = "토큰 없음 또는 만료", content = @Content),
            @ApiResponse(responseCode = "403", description = "ADMIN이 아님", content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일", content = @Content)
    })
    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse addMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddMemberRequest request) {
        return companyService.addMember(principal.companyId(), request);
    }
}

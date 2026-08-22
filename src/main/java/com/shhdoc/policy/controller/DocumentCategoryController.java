package com.shhdoc.policy.controller;

import com.shhdoc.auth.UserPrincipal;
import com.shhdoc.policy.dto.CategoryRequest;
import com.shhdoc.policy.dto.CategoryResponse;
import com.shhdoc.policy.service.DocumentCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "정책 - 문서 대분류", description = "회사별 문서 대분류 관리. 회사 생성 시 기본값이 채워지며 이후 자유롭게 수정한다. ADMIN 전용.")
@RestController
@RequestMapping("/admin/policy/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DocumentCategoryController {

    private final DocumentCategoryService categoryService;

    @Operation(summary = "대분류 목록")
    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return categoryService.list(principal.companyId()).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Operation(summary = "대분류 추가")
    @ApiResponses(@ApiResponse(responseCode = "409", description = "코드 중복", content = @Content))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CategoryRequest request) {
        return CategoryResponse.from(categoryService.create(principal.companyId(), request.code(), request.name()));
    }

    @Operation(summary = "대분류 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "코드 중복", content = @Content)
    })
    @PutMapping("/{categoryId}")
    public CategoryResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        return CategoryResponse.from(
                categoryService.update(principal.companyId(), categoryId, request.code(), request.name()));
    }

    @Operation(summary = "대분류 삭제", description = "하위 문서 유형이나 이 대분류를 조건으로 쓰는 규칙이 있으면 거부된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "대상 없음", content = @Content),
            @ApiResponse(responseCode = "409", description = "하위 유형 또는 참조 규칙 존재", content = @Content)
    })
    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long categoryId) {
        categoryService.delete(principal.companyId(), categoryId);
    }
}

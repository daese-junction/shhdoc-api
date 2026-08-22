package com.shhdoc.user.dto;

import com.shhdoc.user.Role;
import com.shhdoc.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
        Long id,

        @Schema(example = "bob@shhdoc.com")
        String email,

        @Schema(example = "박직원")
        String name,

        @Schema(example = "영업팀")
        String department,

        @Schema(example = "대리")
        String position,

        @Schema(description = "ADMIN = 회사를 만든 대표자(직원 추가·메일 승인 가능), USER = 일반 직원")
        Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(),
                user.getDepartment(), user.getPosition(), user.getRole());
    }
}

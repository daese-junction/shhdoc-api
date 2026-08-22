package com.shhdoc.company.dto;

import com.shhdoc.user.dto.UserResponse;

public record CreateCompanyResponse(CompanyResponse company, UserResponse user) {
}

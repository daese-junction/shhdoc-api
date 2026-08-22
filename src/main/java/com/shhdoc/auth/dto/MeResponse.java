package com.shhdoc.auth.dto;

import com.shhdoc.company.dto.CompanyResponse;
import com.shhdoc.user.Role;

public record MeResponse(Long id, String email, String name, String department, String position,
                         Role role, CompanyResponse company) {
}

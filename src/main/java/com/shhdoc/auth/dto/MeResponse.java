package com.shhdoc.auth.dto;

import com.shhdoc.company.dto.CompanyResponse;
import com.shhdoc.user.Role;

public record MeResponse(Long id, String email, String name, Role role, CompanyResponse company) {
}

package com.ps27.railway.dto;

import com.ps27.railway.enums.Role;

public record UserResponse(Long id, String username, String email, String fullName,
                           Role role, boolean active, Long departmentId, String departmentName) {
}

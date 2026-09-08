package com.ps27.railway.dto;

import com.ps27.railway.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 120) String fullName,
        @NotNull Role role,
        Boolean active,
        Long departmentId) {
}

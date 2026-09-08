package com.ps27.railway.dto;

import com.ps27.railway.enums.ConstraintType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConstraintRequest(
        @NotNull ConstraintType constraintType,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        Long taskId,
        Boolean active) {
}

package com.ps27.railway.dto;

import com.ps27.railway.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MaintenanceResourceRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull ResourceType resourceType,
        Long departmentId,
        @NotNull @Positive Integer capacityPerShift,
        Boolean active) {
}

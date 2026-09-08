package com.ps27.railway.dto;

import com.ps27.railway.enums.BlockRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BlockRequestRequest(
        @NotBlank @Size(max = 30) String blockCode,
        @NotNull Long maintenanceTaskId,
        @NotNull Long corridorId,
        @NotNull LocalDate requestedStart,
        @NotNull LocalDate requestedEnd,
        BlockRequestStatus status,
        @Size(max = 1000) String notes) {
}

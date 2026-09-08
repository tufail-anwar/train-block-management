package com.ps27.railway.dto;

import com.ps27.railway.enums.CorridorStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CorridorRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String startLocation,
        @NotBlank @Size(max = 100) String endLocation,
        @NotNull @Positive double lengthKm,
        CorridorStatus status) {
}

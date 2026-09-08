package com.ps27.railway.dto;

import com.ps27.railway.enums.AssetStatus;
import com.ps27.railway.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AssetRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull AssetType assetType,
        @NotNull Long corridorId,
        @PositiveOrZero Double trackKm,
        AssetStatus status) {
}

package com.ps27.railway.dto;

import com.ps27.railway.enums.AssetStatus;
import com.ps27.railway.enums.AssetType;

public record AssetResponse(Long id, String code, String name, AssetType assetType,
                            Long corridorId, String corridorCode, Double trackKm, AssetStatus status) {
}

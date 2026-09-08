package com.ps27.railway.dto;

import com.ps27.railway.enums.CorridorStatus;

public record CorridorResponse(Long id, String code, String name, String startLocation,
                               String endLocation, double lengthKm, CorridorStatus status) {
}

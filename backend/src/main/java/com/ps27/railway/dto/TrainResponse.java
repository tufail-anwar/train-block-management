package com.ps27.railway.dto;

import com.ps27.railway.enums.TrainType;

public record TrainResponse(Long id, String code, String name, TrainType trainType, boolean active) {
}

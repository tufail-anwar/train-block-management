package com.ps27.railway.dto;

import com.ps27.railway.enums.ConstraintType;

public record ConstraintResponse(Long id, ConstraintType constraintType, String name,
                                 String description, Long taskId, String taskCode, boolean active) {
}

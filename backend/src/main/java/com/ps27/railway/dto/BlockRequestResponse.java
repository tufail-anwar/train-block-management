package com.ps27.railway.dto;

import com.ps27.railway.enums.BlockRequestStatus;

import java.time.LocalDate;

public record BlockRequestResponse(Long id, String blockCode, Long maintenanceTaskId,
                                   String taskCode, String taskTitle, Long corridorId,
                                   String corridorCode, LocalDate requestedStart,
                                   LocalDate requestedEnd, BlockRequestStatus status, String notes) {
}

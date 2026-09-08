package com.ps27.railway.dto;

import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;

import java.time.LocalDate;
import java.util.Set;

public record MaintenanceTaskResponse(
        Long id, String taskCode, String title, String description, TaskType taskType,
        Priority priority, int durationMinutes, Long corridorId, String corridorCode,
        Long assetId, String assetCode, Long departmentId, String departmentName,
        LocalDate requestedStart, LocalDate requestedEnd, LocalDate earliestStart,
        LocalDate latestEnd, TaskStatus status, String notes, Set<Long> dependencyIds) {
}

package com.ps27.railway.dto;

import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record MaintenanceTaskRequest(
        @NotBlank @Size(max = 30) String taskCode,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @NotNull TaskType taskType,
        @NotNull Priority priority,
        @NotNull @Positive Integer durationMinutes,
        @NotNull Long corridorId,
        Long assetId,
        Long departmentId,
        LocalDate requestedStart,
        LocalDate requestedEnd,
        LocalDate earliestStart,
        LocalDate latestEnd,
        TaskStatus status,
        @Size(max = 1000) String notes,
        Set<Long> dependencyIds) {
}

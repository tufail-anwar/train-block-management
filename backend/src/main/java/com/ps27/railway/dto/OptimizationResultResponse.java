package com.ps27.railway.dto;

import com.ps27.railway.enums.OptimizationDecision;

import java.time.LocalDate;
import java.util.List;

/**
 * Per-task outcome of an optimization run. `conflicts` holds the full list of
 * deterministic conflicts evaluated for the chosen candidate (empty when clean);
 * rejected candidate date ranges with reasons are available on the run detail.
 */
public record OptimizationResultResponse(
        Long id,
        Long taskId,
        String taskCode,
        String taskTitle,
        String priority,
        OptimizationDecision decision,
        String reason,
        Integer score,
        Long blockId,
        String blockCode,
        LocalDate scheduledStart,
        LocalDate scheduledEnd,
        List<ConflictResponse> conflicts) {
}

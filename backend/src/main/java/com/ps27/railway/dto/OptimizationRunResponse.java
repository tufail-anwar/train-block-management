package com.ps27.railway.dto;

import com.ps27.railway.enums.OptimizationStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Summary of an optimization run. Detailed per-task outcomes are exposed via
 * {@link OptimizationResultResponse} / {@link OptimizationRunDetailResponse}.
 */
public record OptimizationRunResponse(
        Long id,
        String algorithm,
        OptimizationStatus status,
        LocalDate windowStart,
        LocalDate windowEnd,
        int objectiveScore,
        int tasksScheduled,
        int tasksRejected,
        int conflictsDetected,
        int candidatesEvaluated,
        String summaryReason,
        Instant createdAt) {
}

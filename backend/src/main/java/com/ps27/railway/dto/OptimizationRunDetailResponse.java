package com.ps27.railway.dto;

import com.ps27.railway.enums.OptimizationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Full optimization run: the run summary plus all per-task results.
 * Returned by POST /api/optimization/run.
 */
public record OptimizationRunDetailResponse(
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
        List<String> rejectedCandidates,
        Instant createdAt,
        List<OptimizationResultResponse> results) {
}

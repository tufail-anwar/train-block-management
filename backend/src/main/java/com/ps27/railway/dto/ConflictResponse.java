package com.ps27.railway.dto;

import com.ps27.railway.enums.ConflictSeverity;
import com.ps27.railway.enums.ConflictType;

import java.time.LocalDate;

/**
 * A single detected conflict. Carries the deterministic type, severity,
 * the affected block/task/corridor, the offending entity, time/location
 * info and a human-readable explanation.
 */
public record ConflictResponse(
        Long id,
        ConflictType type,
        ConflictSeverity severity,
        Long blockId,
        String blockCode,
        Long taskId,
        String taskCode,
        String taskTitle,
        Long corridorId,
        String corridorCode,
        LocalDate date,
        String details,
        String explanation) {

    /** Convenience factory for a conflict not tied to a persisted block (what-if checks). */
    public static ConflictResponse of(ConflictType type, ConflictSeverity severity,
                                      Long blockId, String blockCode,
                                      Long taskId, String taskCode, String taskTitle,
                                      Long corridorId, String corridorCode,
                                      LocalDate date, String details, String explanation) {
        return new ConflictResponse(null, type, severity, blockId, blockCode,
                taskId, taskCode, taskTitle, corridorId, corridorCode,
                date, details, explanation);
    }
}

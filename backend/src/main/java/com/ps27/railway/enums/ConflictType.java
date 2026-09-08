package com.ps27.railway.enums;

/**
 * Deterministic conflict types detected by the conflict detection module
 * (Part 4). Each maps to a hard constraint from the project docs or a
 * user requirement.
 */
public enum ConflictType {
    /** Active block overlaps a TrainSchedule on the same corridor. */
    TRAIN_OVERLAP,
    /** Two active blocks on the same corridor with overlapping date ranges. */
    BLOCK_OVERLAP,
    /** Two overlapping blocks whose tasks target the same asset. */
    SPATIAL_OVERLAP,
    /** Consecutive blocks on the same corridor with gap below the configured minimum. */
    INSUFFICIENT_GAP,
    /** Active blocks exceed a department's combined resource capacity on a day. */
    RESOURCE_CONFLICT,
    /** Block violates a configured operational/safety rule or allowed window. */
    CONSTRAINT_VIOLATION,
    /** A single-day block whose duration exceeds the configured daily work limit. */
    DURATION_FIT
}

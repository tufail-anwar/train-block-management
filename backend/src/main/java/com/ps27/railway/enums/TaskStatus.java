package com.ps27.railway.enums;

/** Lifecycle status of a maintenance task. */
public enum TaskStatus {
    DRAFT,
    REQUESTED,
    APPROVED,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

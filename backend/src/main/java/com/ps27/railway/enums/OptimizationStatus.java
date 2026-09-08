package com.ps27.railway.enums;

/**
 * Lifecycle status of an optimization run (Part 5 / plan part3).
 * Runs are executed synchronously and completed in the same request,
 * but the status is persisted for auditability and future async runs.
 */
public enum OptimizationStatus {
    RUNNING,
    COMPLETED,
    FAILED
}

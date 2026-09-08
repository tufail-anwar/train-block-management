package com.ps27.railway.enums;

/**
 * Deterministic decision produced by the optimizer for one maintenance task.
 * A task is SCHEDULED when a feasible candidate block was found, otherwise
 * REJECTED with an explainable reason.
 */
public enum OptimizationDecision {
    SCHEDULED,
    REJECTED
}

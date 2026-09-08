package com.ps27.railway.enums;

/**
 * Types of maintenance requirements/constraints.
 * Stored now; interpreted later by conflict detection and optimization.
 */
public enum ConstraintType {
    TIME_WINDOW,
    CORRIDOR_AVAILABILITY,
    RESOURCE_CAPACITY,
    DEPENDENCY,
    SAFETY_RULE,
    OPERATIONAL_RULE
}

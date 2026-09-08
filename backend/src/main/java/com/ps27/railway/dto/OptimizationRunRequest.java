package com.ps27.railway.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Request to run the deterministic block optimizer. The optimization window is
 * mandatory; all other fields are optional tuning knobs with deterministic defaults.
 */
public record OptimizationRunRequest(
        @NotNull LocalDate windowStart,
        @NotNull LocalDate windowEnd,
        List<Long> taskIds,
        Integer maxConcurrentBlocks,
        List<@Valid BlackoutWindow> blackoutWindows,
        Boolean persistBlocks) {

    /** A hard "no maintenance" window. Any candidate overlapping it is rejected. */
    public record BlackoutWindow(
            @NotNull LocalDate from,
            @NotNull LocalDate to) {

        public BlackoutWindow {
            if (to.isBefore(from)) {
                throw new IllegalArgumentException("Blackout 'to' must be after or equal to 'from'");
            }
        }
    }
}

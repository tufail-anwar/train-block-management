package com.ps27.railway.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only availability view for a corridor over a date range:
 * planned train runs and requested maintenance blocks. No conflict analysis
 * is performed in Part 1.
 */
public record CorridorAvailabilityResponse(
        Long corridorId,
        String corridorCode,
        String corridorName,
        LocalDate from,
        LocalDate to,
        List<TrainRun> trains,
        List<Block> blocks) {

    public record TrainRun(Long scheduleId, String trainCode, String trainName,
                           LocalDate scheduleDate, java.time.LocalTime departureTime,
                           java.time.LocalTime arrivalTime, String direction) {
    }

    public record Block(Long blockId, String blockCode, String taskCode, String taskTitle,
                        LocalDate requestedStart, LocalDate requestedEnd, String status) {
    }
}

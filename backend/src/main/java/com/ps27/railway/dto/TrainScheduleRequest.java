package com.ps27.railway.dto;

import com.ps27.railway.enums.Direction;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record TrainScheduleRequest(
        @NotNull Long trainId,
        @NotNull Long corridorId,
        LocalDate scheduleDate,
        DayOfWeek dayOfWeek,
        @NotNull LocalTime departureTime,
        @NotNull LocalTime arrivalTime,
        @NotNull Direction direction) {
}

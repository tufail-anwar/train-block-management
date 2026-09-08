package com.ps27.railway.dto;

import com.ps27.railway.enums.Direction;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record TrainScheduleResponse(Long id, Long trainId, String trainCode, String trainName,
                                    Long corridorId, String corridorCode, LocalDate scheduleDate,
                                    DayOfWeek dayOfWeek, LocalTime departureTime,
                                    LocalTime arrivalTime, Direction direction) {
}

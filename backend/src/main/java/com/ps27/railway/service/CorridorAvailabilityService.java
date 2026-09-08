package com.ps27.railway.service;

import com.ps27.railway.dto.CorridorAvailabilityResponse;
import com.ps27.railway.entity.BlockRequest;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.TrainSchedule;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.BlockRequestRepository;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.TrainScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only corridor availability view. This intentionally does NOT perform
 * conflict detection or optimization — it only lists planned train runs and
 * requested maintenance blocks in a date range.
 */
@Service
public class CorridorAvailabilityService {

    private final CorridorRepository corridorRepository;
    private final TrainScheduleRepository trainScheduleRepository;
    private final BlockRequestRepository blockRequestRepository;

    public CorridorAvailabilityService(CorridorRepository corridorRepository,
                                       TrainScheduleRepository trainScheduleRepository,
                                       BlockRequestRepository blockRequestRepository) {
        this.corridorRepository = corridorRepository;
        this.trainScheduleRepository = trainScheduleRepository;
        this.blockRequestRepository = blockRequestRepository;
    }

    @Transactional(readOnly = true)
    public CorridorAvailabilityResponse getAvailability(Long corridorId, LocalDate from, LocalDate to) {
        Corridor corridor = corridorRepository.findById(corridorId)
                .orElseThrow(() -> new ResourceNotFoundException("Corridor not found: " + corridorId));
        if (from == null || to == null) {
            throw new BadRequestException("Both 'from' and 'to' query parameters are required");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("'to' must be after or equal to 'from'");
        }

        List<TrainSchedule> schedules = trainScheduleRepository
                .findByCorridorIdAndScheduleDateBetween(corridorId, from, to);
        List<BlockRequest> blocks = blockRequestRepository
                .findByCorridorIdAndRequestedStartBetween(corridorId, from, to);

        List<CorridorAvailabilityResponse.TrainRun> trainRuns = schedules.stream()
                .map(s -> new CorridorAvailabilityResponse.TrainRun(
                        s.getId(), s.getTrain().getCode(), s.getTrain().getName(),
                        s.getScheduleDate(), s.getDepartureTime(), s.getArrivalTime(),
                        s.getDirection().name()))
                .toList();

        List<CorridorAvailabilityResponse.Block> blockList = blocks.stream()
                .map(b -> new CorridorAvailabilityResponse.Block(
                        b.getId(), b.getBlockCode(), b.getMaintenanceTask().getTaskCode(),
                        b.getMaintenanceTask().getTitle(),
                        b.getRequestedStart(), b.getRequestedEnd(), b.getStatus().name()))
                .toList();

        return new CorridorAvailabilityResponse(corridor.getId(), corridor.getCode(),
                corridor.getName(), from, to, trainRuns, blockList);
    }
}

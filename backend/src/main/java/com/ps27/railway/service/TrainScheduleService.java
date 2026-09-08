package com.ps27.railway.service;

import com.ps27.railway.dto.TrainScheduleRequest;
import com.ps27.railway.dto.TrainScheduleResponse;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.Train;
import com.ps27.railway.entity.TrainSchedule;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.TrainRepository;
import com.ps27.railway.repository.TrainScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TrainScheduleService {

    private final TrainScheduleRepository repository;
    private final TrainRepository trainRepository;
    private final CorridorRepository corridorRepository;

    public TrainScheduleService(TrainScheduleRepository repository,
                                TrainRepository trainRepository,
                                CorridorRepository corridorRepository) {
        this.repository = repository;
        this.trainRepository = trainRepository;
        this.corridorRepository = corridorRepository;
    }

    @Transactional(readOnly = true)
    public List<TrainScheduleResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TrainScheduleResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public TrainScheduleResponse create(TrainScheduleRequest request) {
        validate(request);
        Train train = train(request.trainId());
        Corridor corridor = corridor(request.corridorId());
        TrainSchedule entity = new TrainSchedule(train, corridor, request.scheduleDate(),
                request.dayOfWeek(), request.departureTime(), request.arrivalTime(),
                request.direction());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public TrainScheduleResponse update(Long id, TrainScheduleRequest request) {
        validate(request);
        TrainSchedule entity = getEntity(id);
        entity.setTrain(train(request.trainId()));
        entity.setCorridor(corridor(request.corridorId()));
        entity.setScheduleDate(request.scheduleDate());
        entity.setDayOfWeek(request.dayOfWeek());
        entity.setDepartureTime(request.departureTime());
        entity.setArrivalTime(request.arrivalTime());
        entity.setDirection(request.direction());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        TrainSchedule entity = getEntity(id);
        repository.delete(entity);
    }

    private void validate(TrainScheduleRequest request) {
        if (request.arrivalTime().isBefore(request.departureTime())) {
            throw new BadRequestException("arrivalTime must be after departureTime");
        }
        if (request.scheduleDate() == null && request.dayOfWeek() == null) {
            throw new BadRequestException("Either scheduleDate or dayOfWeek must be provided");
        }
        if (request.scheduleDate() != null && request.dayOfWeek() != null) {
            throw new BadRequestException("Provide either scheduleDate or dayOfWeek, not both");
        }
        if (request.scheduleDate() != null && request.scheduleDate().isBefore(LocalDate.now().minusDays(1))) {
            throw new BadRequestException("scheduleDate must not be in the past");
        }
    }

    private TrainSchedule getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train schedule not found: " + id));
    }

    private Train train(Long id) {
        return trainRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Train not found: " + id));
    }

    private Corridor corridor(Long id) {
        return corridorRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Corridor not found: " + id));
    }

    private TrainScheduleResponse toResponse(TrainSchedule s) {
        return new TrainScheduleResponse(s.getId(), s.getTrain().getId(), s.getTrain().getCode(),
                s.getTrain().getName(), s.getCorridor().getId(), s.getCorridor().getCode(),
                s.getScheduleDate(), s.getDayOfWeek(), s.getDepartureTime(), s.getArrivalTime(),
                s.getDirection());
    }
}

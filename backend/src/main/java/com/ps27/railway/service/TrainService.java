package com.ps27.railway.service;

import com.ps27.railway.dto.TrainRequest;
import com.ps27.railway.dto.TrainResponse;
import com.ps27.railway.entity.Train;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.TrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainService {

    private final TrainRepository repository;

    public TrainService(TrainRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TrainResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TrainResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public TrainResponse create(TrainRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new BadRequestException("Train code already exists: " + request.code());
        }
        Train entity = new Train(request.code(), request.name(), request.trainType(),
                request.active() == null || request.active());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public TrainResponse update(Long id, TrainRequest request) {
        Train entity = getEntity(id);
        repository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("Train code already exists: " + request.code());
                });
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setTrainType(request.trainType());
        entity.setActive(request.active() == null || request.active());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        Train entity = getEntity(id);
        repository.delete(entity);
    }

    private Train getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found: " + id));
    }

    private TrainResponse toResponse(Train t) {
        return new TrainResponse(t.getId(), t.getCode(), t.getName(), t.getTrainType(), t.isActive());
    }
}

package com.ps27.railway.service;

import com.ps27.railway.dto.CorridorRequest;
import com.ps27.railway.dto.CorridorResponse;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.CorridorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CorridorService {

    private final CorridorRepository repository;

    public CorridorService(CorridorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CorridorResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CorridorResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public CorridorResponse create(CorridorRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new BadRequestException("Corridor code already exists: " + request.code());
        }
        Corridor entity = new Corridor(request.code(), request.name(), request.startLocation(),
                request.endLocation(), request.lengthKm(),
                request.status() != null ? request.status() : CorridorStatus.OPERATIONAL);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public CorridorResponse update(Long id, CorridorRequest request) {
        Corridor entity = getEntity(id);
        repository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("Corridor code already exists: " + request.code());
                });
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setStartLocation(request.startLocation());
        entity.setEndLocation(request.endLocation());
        entity.setLengthKm(request.lengthKm());
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        Corridor entity = getEntity(id);
        repository.delete(entity);
    }

    private Corridor getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Corridor not found: " + id));
    }

    private CorridorResponse toResponse(Corridor c) {
        return new CorridorResponse(c.getId(), c.getCode(), c.getName(), c.getStartLocation(),
                c.getEndLocation(), c.getLengthKm(), c.getStatus());
    }
}

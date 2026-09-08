package com.ps27.railway.service;

import com.ps27.railway.dto.BlockRequestRequest;
import com.ps27.railway.dto.BlockRequestResponse;
import com.ps27.railway.entity.BlockRequest;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.enums.BlockRequestStatus;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.BlockRequestRepository;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BlockRequestService {

    private final BlockRequestRepository repository;
    private final MaintenanceTaskRepository taskRepository;
    private final CorridorRepository corridorRepository;

    public BlockRequestService(BlockRequestRepository repository,
                               MaintenanceTaskRepository taskRepository,
                               CorridorRepository corridorRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.corridorRepository = corridorRepository;
    }

    @Transactional(readOnly = true)
    public List<BlockRequestResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BlockRequestResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public BlockRequestResponse create(BlockRequestRequest request) {
        if (repository.existsByBlockCode(request.blockCode())) {
            throw new BadRequestException("Block code already exists: " + request.blockCode());
        }
        validateDates(request);
        MaintenanceTask task = task(request.maintenanceTaskId());
        Corridor corridor = corridor(request.corridorId());
        BlockRequest entity = new BlockRequest(request.blockCode(), task, corridor,
                request.requestedStart(), request.requestedEnd(),
                request.status() != null ? request.status() : BlockRequestStatus.DRAFT,
                request.notes());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public BlockRequestResponse update(Long id, BlockRequestRequest request) {
        BlockRequest entity = getEntity(id);
        repository.findByBlockCode(request.blockCode())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("Block code already exists: " + request.blockCode());
                });
        validateDates(request);
        entity.setBlockCode(request.blockCode());
        entity.setMaintenanceTask(task(request.maintenanceTaskId()));
        entity.setCorridor(corridor(request.corridorId()));
        entity.setRequestedStart(request.requestedStart());
        entity.setRequestedEnd(request.requestedEnd());
        entity.setStatus(request.status() != null ? request.status() : entity.getStatus());
        entity.setNotes(request.notes());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        BlockRequest entity = getEntity(id);
        repository.delete(entity);
    }

    private void validateDates(BlockRequestRequest request) {
        if (request.requestedEnd().isBefore(request.requestedStart())) {
            throw new BadRequestException("requestedEnd must be after or equal to requestedStart");
        }
    }

    private BlockRequest getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Block request not found: " + id));
    }

    private MaintenanceTask task(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Maintenance task not found: " + id));
    }

    private Corridor corridor(Long id) {
        return corridorRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Corridor not found: " + id));
    }

    private BlockRequestResponse toResponse(BlockRequest b) {
        MaintenanceTask t = b.getMaintenanceTask();
        Corridor c = b.getCorridor();
        return new BlockRequestResponse(b.getId(), b.getBlockCode(),
                t.getId(), t.getTaskCode(), t.getTitle(),
                c.getId(), c.getCode(),
                b.getRequestedStart(), b.getRequestedEnd(), b.getStatus(), b.getNotes());
    }
}

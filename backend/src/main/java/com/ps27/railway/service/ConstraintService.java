package com.ps27.railway.service;

import com.ps27.railway.dto.ConstraintRequest;
import com.ps27.railway.dto.ConstraintResponse;
import com.ps27.railway.entity.Constraint;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.ConstraintRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConstraintService {

    private final ConstraintRepository repository;
    private final MaintenanceTaskRepository taskRepository;

    public ConstraintService(ConstraintRepository repository, MaintenanceTaskRepository taskRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<ConstraintResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ConstraintResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public ConstraintResponse create(ConstraintRequest request) {
        Constraint entity = new Constraint(request.constraintType(), request.name(),
                request.description(), task(request.taskId()),
                request.active() == null || request.active());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public ConstraintResponse update(Long id, ConstraintRequest request) {
        Constraint entity = getEntity(id);
        entity.setConstraintType(request.constraintType());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setTask(task(request.taskId()));
        entity.setActive(request.active() == null || request.active());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        Constraint entity = getEntity(id);
        repository.delete(entity);
    }

    private Constraint getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Constraint not found: " + id));
    }

    private MaintenanceTask task(Long id) {
        if (id == null) {
            return null;
        }
        return taskRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Maintenance task not found: " + id));
    }

    private ConstraintResponse toResponse(Constraint c) {
        MaintenanceTask t = c.getTask();
        return new ConstraintResponse(c.getId(), c.getConstraintType(), c.getName(),
                c.getDescription(),
                t != null ? t.getId() : null, t != null ? t.getTaskCode() : null,
                c.isActive());
    }
}

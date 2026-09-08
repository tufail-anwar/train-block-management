package com.ps27.railway.service;

import com.ps27.railway.dto.MaintenanceTaskRequest;
import com.ps27.railway.dto.MaintenanceTaskResponse;
import com.ps27.railway.entity.Asset;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.Department;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.AssetRepository;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.DepartmentRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MaintenanceTaskService {

    private final MaintenanceTaskRepository repository;
    private final CorridorRepository corridorRepository;
    private final AssetRepository assetRepository;
    private final DepartmentRepository departmentRepository;

    public MaintenanceTaskService(MaintenanceTaskRepository repository,
                                  CorridorRepository corridorRepository,
                                  AssetRepository assetRepository,
                                  DepartmentRepository departmentRepository) {
        this.repository = repository;
        this.corridorRepository = corridorRepository;
        this.assetRepository = assetRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTaskResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceTaskResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public MaintenanceTaskResponse create(MaintenanceTaskRequest request) {
        if (repository.existsByTaskCode(request.taskCode())) {
            throw new BadRequestException("Task code already exists: " + request.taskCode());
        }
        validateDates(request);
        Corridor corridor = corridor(request.corridorId());
        Asset asset = asset(request.assetId());
        Department department = department(request.departmentId());

        MaintenanceTask entity = new MaintenanceTask(request.taskCode(), request.title(),
                request.description(), request.taskType(), request.priority(),
                request.durationMinutes(), corridor, asset, department,
                request.requestedStart(), request.requestedEnd(),
                request.earliestStart(), request.latestEnd(),
                request.status() != null ? request.status() : TaskStatus.DRAFT,
                request.notes());
        entity.setDependencies(resolveDependencies(request.dependencyIds(), null));
        return toResponse(repository.save(entity));
    }

    @Transactional
    public MaintenanceTaskResponse update(Long id, MaintenanceTaskRequest request) {
        MaintenanceTask entity = getEntity(id);
        repository.findByTaskCode(request.taskCode())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("Task code already exists: " + request.taskCode());
                });
        validateDates(request);
        entity.setTaskCode(request.taskCode());
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setTaskType(request.taskType());
        entity.setPriority(request.priority());
        entity.setDurationMinutes(request.durationMinutes());
        entity.setCorridor(corridor(request.corridorId()));
        entity.setAsset(asset(request.assetId()));
        entity.setDepartment(department(request.departmentId()));
        entity.setRequestedStart(request.requestedStart());
        entity.setRequestedEnd(request.requestedEnd());
        entity.setEarliestStart(request.earliestStart());
        entity.setLatestEnd(request.latestEnd());
        entity.setStatus(request.status() != null ? request.status() : entity.getStatus());
        entity.setNotes(request.notes());
        entity.setDependencies(resolveDependencies(request.dependencyIds(), id));
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        MaintenanceTask entity = getEntity(id);
        repository.delete(entity);
    }

    private void validateDates(MaintenanceTaskRequest request) {
        if (request.requestedStart() != null && request.requestedEnd() != null
                && request.requestedEnd().isBefore(request.requestedStart())) {
            throw new BadRequestException("requestedEnd must be after or equal to requestedStart");
        }
        if (request.earliestStart() != null && request.latestEnd() != null
                && request.latestEnd().isBefore(request.earliestStart())) {
            throw new BadRequestException("latestEnd must be after or equal to earliestStart");
        }
    }

    private Set<MaintenanceTask> resolveDependencies(Set<Long> dependencyIds, Long selfId) {
        Set<MaintenanceTask> deps = new HashSet<>();
        if (dependencyIds == null || dependencyIds.isEmpty()) {
            return deps;
        }
        for (Long depId : dependencyIds) {
            if (depId.equals(selfId)) {
                throw new BadRequestException("A task cannot depend on itself");
            }
            MaintenanceTask dep = repository.findById(depId)
                    .orElseThrow(() -> new BadRequestException("Dependency task not found: " + depId));
            deps.add(dep);
        }
        return deps;
    }

    private MaintenanceTask getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance task not found: " + id));
    }

    private Corridor corridor(Long id) {
        return corridorRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Corridor not found: " + id));
    }

    private Asset asset(Long id) {
        if (id == null) {
            return null;
        }
        return assetRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Asset not found: " + id));
    }

    private Department department(Long id) {
        if (id == null) {
            return null;
        }
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Department not found: " + id));
    }

    private MaintenanceTaskResponse toResponse(MaintenanceTask t) {
        Corridor c = t.getCorridor();
        Asset a = t.getAsset();
        Department d = t.getDepartment();
        Set<Long> depIds = t.getDependencies().stream()
                .map(MaintenanceTask::getId)
                .collect(java.util.stream.Collectors.toSet());
        return new MaintenanceTaskResponse(
                t.getId(), t.getTaskCode(), t.getTitle(), t.getDescription(), t.getTaskType(),
                t.getPriority(), t.getDurationMinutes(),
                c != null ? c.getId() : null, c != null ? c.getCode() : null,
                a != null ? a.getId() : null, a != null ? a.getCode() : null,
                d != null ? d.getId() : null, d != null ? d.getName() : null,
                t.getRequestedStart(), t.getRequestedEnd(), t.getEarliestStart(), t.getLatestEnd(),
                t.getStatus(), t.getNotes(), depIds);
    }
}

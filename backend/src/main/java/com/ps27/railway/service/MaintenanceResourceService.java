package com.ps27.railway.service;

import com.ps27.railway.dto.MaintenanceResourceRequest;
import com.ps27.railway.dto.MaintenanceResourceResponse;
import com.ps27.railway.entity.Department;
import com.ps27.railway.entity.MaintenanceResource;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.DepartmentRepository;
import com.ps27.railway.repository.MaintenanceResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaintenanceResourceService {

    private final MaintenanceResourceRepository repository;
    private final DepartmentRepository departmentRepository;

    public MaintenanceResourceService(MaintenanceResourceRepository repository,
                                      DepartmentRepository departmentRepository) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResourceResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceResourceResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public MaintenanceResourceResponse create(MaintenanceResourceRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new BadRequestException("Resource code already exists: " + request.code());
        }
        MaintenanceResource entity = new MaintenanceResource(request.code(), request.name(),
                request.resourceType(), department(request.departmentId()),
                request.capacityPerShift(), request.active() == null || request.active());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public MaintenanceResourceResponse update(Long id, MaintenanceResourceRequest request) {
        MaintenanceResource entity = getEntity(id);
        repository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("Resource code already exists: " + request.code());
                });
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setResourceType(request.resourceType());
        entity.setDepartment(department(request.departmentId()));
        entity.setCapacityPerShift(request.capacityPerShift());
        entity.setActive(request.active() == null || request.active());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        MaintenanceResource entity = getEntity(id);
        repository.delete(entity);
    }

    private MaintenanceResource getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
    }

    private Department department(Long id) {
        if (id == null) {
            return null;
        }
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Department not found: " + id));
    }

    private MaintenanceResourceResponse toResponse(MaintenanceResource r) {
        Department d = r.getDepartment();
        return new MaintenanceResourceResponse(r.getId(), r.getCode(), r.getName(),
                r.getResourceType(),
                d != null ? d.getId() : null, d != null ? d.getName() : null,
                r.getCapacityPerShift(), r.isActive());
    }
}

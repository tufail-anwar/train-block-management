package com.ps27.railway.service;

import com.ps27.railway.dto.DepartmentRequest;
import com.ps27.railway.dto.DepartmentResponse;
import com.ps27.railway.entity.Department;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository repository;

    public DepartmentService(DepartmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new BadRequestException("Department code already exists: " + request.code());
        }
        Department entity = new Department(request.code(), request.name(), request.description());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department entity = getEntity(id);
        repository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("Department code already exists: " + request.code());
                });
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        Department entity = getEntity(id);
        repository.delete(entity);
    }

    private Department getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getCode(), d.getName(), d.getDescription());
    }
}

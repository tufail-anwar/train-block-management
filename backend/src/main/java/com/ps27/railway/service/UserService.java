package com.ps27.railway.service;

import com.ps27.railway.dto.UserRequest;
import com.ps27.railway.dto.UserResponse;
import com.ps27.railway.entity.Department;
import com.ps27.railway.entity.User;
import com.ps27.railway.enums.Role;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.DepartmentRepository;
import com.ps27.railway.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;
    private final DepartmentRepository departmentRepository;

    public UserService(UserRepository repository, DepartmentRepository departmentRepository) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already exists: " + request.username());
        }
        if (repository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists: " + request.email());
        }
        User entity = new User(request.username(), request.email(), request.fullName(),
                roleOrDefault(request.role()), department(request.departmentId()));
        entity.setActive(request.active() == null || request.active());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User entity = getEntity(id);
        repository.findByUsername(request.username())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("Username already exists: " + request.username());
                });
        repository.findByEmail(request.email())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("Email already exists: " + request.email());
                });
        entity.setUsername(request.username());
        entity.setEmail(request.email());
        entity.setFullName(request.fullName());
        entity.setRole(roleOrDefault(request.role()));
        entity.setDepartment(department(request.departmentId()));
        entity.setActive(request.active() == null || request.active());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        User entity = getEntity(id);
        repository.delete(entity);
    }

    private User getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Role roleOrDefault(Role role) {
        return role != null ? role : Role.DEPARTMENT_USER;
    }

    private Department department(Long id) {
        if (id == null) {
            return null;
        }
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Department not found: " + id));
    }

    private UserResponse toResponse(User u) {
        Department dept = u.getDepartment();
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                u.getRole(), u.isActive(),
                dept != null ? dept.getId() : null,
                dept != null ? dept.getName() : null);
    }
}

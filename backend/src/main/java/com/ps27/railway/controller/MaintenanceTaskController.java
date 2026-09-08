package com.ps27.railway.controller;

import com.ps27.railway.dto.MaintenanceTaskRequest;
import com.ps27.railway.dto.MaintenanceTaskResponse;
import com.ps27.railway.service.MaintenanceTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceTaskController {

    private final MaintenanceTaskService service;

    public MaintenanceTaskController(MaintenanceTaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<MaintenanceTaskResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MaintenanceTaskResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceTaskResponse create(@Valid @RequestBody MaintenanceTaskRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MaintenanceTaskResponse update(@PathVariable Long id,
                                          @Valid @RequestBody MaintenanceTaskRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

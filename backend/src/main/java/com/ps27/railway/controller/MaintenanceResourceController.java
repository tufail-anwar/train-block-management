package com.ps27.railway.controller;

import com.ps27.railway.dto.MaintenanceResourceRequest;
import com.ps27.railway.dto.MaintenanceResourceResponse;
import com.ps27.railway.service.MaintenanceResourceService;
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
@RequestMapping("/api/resources")
public class MaintenanceResourceController {

    private final MaintenanceResourceService service;

    public MaintenanceResourceController(MaintenanceResourceService service) {
        this.service = service;
    }

    @GetMapping
    public List<MaintenanceResourceResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MaintenanceResourceResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceResourceResponse create(@Valid @RequestBody MaintenanceResourceRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MaintenanceResourceResponse update(@PathVariable Long id,
                                              @Valid @RequestBody MaintenanceResourceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

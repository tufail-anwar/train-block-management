package com.ps27.railway.controller;

import com.ps27.railway.dto.CorridorAvailabilityResponse;
import com.ps27.railway.dto.CorridorRequest;
import com.ps27.railway.dto.CorridorResponse;
import com.ps27.railway.service.CorridorAvailabilityService;
import com.ps27.railway.service.CorridorService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/corridors")
public class CorridorController {

    private final CorridorService service;
    private final CorridorAvailabilityService availabilityService;

    public CorridorController(CorridorService service,
                              CorridorAvailabilityService availabilityService) {
        this.service = service;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public List<CorridorResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CorridorResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/availability")
    public CorridorAvailabilityResponse availability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return availabilityService.getAvailability(id, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CorridorResponse create(@Valid @RequestBody CorridorRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CorridorResponse update(@PathVariable Long id,
                                   @Valid @RequestBody CorridorRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

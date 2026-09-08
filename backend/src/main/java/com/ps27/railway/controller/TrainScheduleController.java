package com.ps27.railway.controller;

import com.ps27.railway.dto.TrainScheduleRequest;
import com.ps27.railway.dto.TrainScheduleResponse;
import com.ps27.railway.service.TrainScheduleService;
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
@RequestMapping("/api/train-schedules")
public class TrainScheduleController {

    private final TrainScheduleService service;

    public TrainScheduleController(TrainScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public List<TrainScheduleResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TrainScheduleResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainScheduleResponse create(@Valid @RequestBody TrainScheduleRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TrainScheduleResponse update(@PathVariable Long id,
                                        @Valid @RequestBody TrainScheduleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

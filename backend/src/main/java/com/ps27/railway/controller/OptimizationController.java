package com.ps27.railway.controller;

import com.ps27.railway.dto.OptimizationResultResponse;
import com.ps27.railway.dto.OptimizationRunDetailResponse;
import com.ps27.railway.dto.OptimizationRunRequest;
import com.ps27.railway.dto.OptimizationRunResponse;
import com.ps27.railway.service.OptimizationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Maintenance block optimization endpoints (Part 5). {@code POST /run} starts a
 * deterministic, explainable optimization run; the run and its per-task results
 * are persisted and retrievable by id.
 */
@RestController
@RequestMapping("/api/optimization")
public class OptimizationController {

    private final OptimizationService service;

    public OptimizationController(OptimizationService service) {
        this.service = service;
    }

    /** Run the deterministic optimizer and return the full run detail (persists run + results). */
    @PostMapping("/run")
    public OptimizationRunDetailResponse run(@Valid @RequestBody OptimizationRunRequest request) {
        return service.run(request);
    }

    /** Retrieve a persisted run summary by id (404 if missing). */
    @GetMapping("/runs/{id}")
    public OptimizationRunResponse getRun(@PathVariable Long id) {
        return service.getRun(id);
    }

    /** Retrieve the per-task results of a persisted run by id (404 if missing). */
    @GetMapping("/runs/{id}/results")
    public List<OptimizationResultResponse> getResults(@PathVariable Long id) {
        return service.getResults(id);
    }
}

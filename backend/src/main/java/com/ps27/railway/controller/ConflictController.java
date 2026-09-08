package com.ps27.railway.controller;

import com.ps27.railway.dto.BlockRequestRequest;
import com.ps27.railway.dto.ConflictCheckResponse;
import com.ps27.railway.dto.ConflictResponse;
import com.ps27.railway.service.ConflictDetectionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only conflict detection endpoints (Part 4). No persistence of
 * detection results; every response is deterministic and explainable.
 */
@RestController
@RequestMapping("/api/conflicts")
public class ConflictController {

    private final ConflictDetectionService service;

    public ConflictController(ConflictDetectionService service) {
        this.service = service;
    }

    /** Detect conflicts for all active blocks, optionally filtered by corridor / date range. */
    @GetMapping
    public List<ConflictResponse> findAll(
            @RequestParam(required = false) Long corridorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.detectAll(corridorId, from, to);
    }

    /** Detect conflicts for a single persisted block (404 if missing). */
    @GetMapping("/{blockId}")
    public List<ConflictResponse> findByBlockId(@PathVariable Long blockId) {
        return service.detectForBlock(blockId);
    }

    /** What-if check: validate a proposed block request without persisting it. */
    @PostMapping("/check")
    public ConflictCheckResponse check(@Valid @RequestBody BlockRequestRequest request) {
        return service.checkProposed(request);
    }
}

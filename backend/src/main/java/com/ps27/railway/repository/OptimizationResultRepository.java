package com.ps27.railway.repository;

import com.ps27.railway.entity.OptimizationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptimizationResultRepository extends JpaRepository<OptimizationResult, Long> {

    List<OptimizationResult> findByRunIdOrderById(Long runId);
}

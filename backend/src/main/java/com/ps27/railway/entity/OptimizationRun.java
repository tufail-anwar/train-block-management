package com.ps27.railway.entity;

import com.ps27.railway.enums.OptimizationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A persisted optimization run (Part 5 / plan part3). Stores the deterministic
 * algorithm used, the window, the objective score, counts, an explainable summary
 * reason and the JSON payload of all rejected candidate date ranges with reasons.
 * Selected candidates are persisted as {@link BlockRequest}s; per-task decisions
 * are stored as {@link OptimizationResult} children.
 */
@Entity
@Table(name = "optimization_runs")
public class OptimizationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "algorithm", nullable = false, length = 50)
    private String algorithm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OptimizationStatus status;

    @Column(name = "window_start", nullable = false)
    private LocalDate windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDate windowEnd;

    @Column(name = "objective_score", nullable = false)
    private int objectiveScore;

    @Column(name = "tasks_scheduled", nullable = false)
    private int tasksScheduled;

    @Column(name = "tasks_rejected", nullable = false)
    private int tasksRejected;

    @Column(name = "conflicts_detected", nullable = false)
    private int conflictsDetected;

    @Column(name = "candidates_evaluated", nullable = false)
    private int candidatesEvaluated;

    @Column(name = "summary_reason", length = 2000)
    private String summaryReason;

    /** JSON array of rejected candidate date ranges with reasons (explainability). */
    @Column(name = "rejected_candidates_json", columnDefinition = "TEXT")
    private String rejectedCandidatesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OptimizationResult> results = new ArrayList<>();

    protected OptimizationRun() {
        // JPA
    }

    public OptimizationRun(String algorithm, OptimizationStatus status,
                           LocalDate windowStart, LocalDate windowEnd,
                           int objectiveScore, int tasksScheduled, int tasksRejected,
                           int conflictsDetected, int candidatesEvaluated,
                           String summaryReason, String rejectedCandidatesJson) {
        this.algorithm = algorithm;
        this.status = status;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.objectiveScore = objectiveScore;
        this.tasksScheduled = tasksScheduled;
        this.tasksRejected = tasksRejected;
        this.conflictsDetected = conflictsDetected;
        this.candidatesEvaluated = candidatesEvaluated;
        this.summaryReason = summaryReason;
        this.rejectedCandidatesJson = rejectedCandidatesJson;
        this.createdAt = Instant.now();
    }

    public void addResult(OptimizationResult result) {
        result.setRun(this);
        this.results.add(result);
    }

    public Long getId() {
        return id;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public OptimizationStatus getStatus() {
        return status;
    }

    public void setStatus(OptimizationStatus status) {
        this.status = status;
    }

    public LocalDate getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDate windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDate getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalDate windowEnd) {
        this.windowEnd = windowEnd;
    }

    public int getObjectiveScore() {
        return objectiveScore;
    }

    public void setObjectiveScore(int objectiveScore) {
        this.objectiveScore = objectiveScore;
    }

    public int getTasksScheduled() {
        return tasksScheduled;
    }

    public void setTasksScheduled(int tasksScheduled) {
        this.tasksScheduled = tasksScheduled;
    }

    public int getTasksRejected() {
        return tasksRejected;
    }

    public void setTasksRejected(int tasksRejected) {
        this.tasksRejected = tasksRejected;
    }

    public int getConflictsDetected() {
        return conflictsDetected;
    }

    public void setConflictsDetected(int conflictsDetected) {
        this.conflictsDetected = conflictsDetected;
    }

    public int getCandidatesEvaluated() {
        return candidatesEvaluated;
    }

    public void setCandidatesEvaluated(int candidatesEvaluated) {
        this.candidatesEvaluated = candidatesEvaluated;
    }

    public String getSummaryReason() {
        return summaryReason;
    }

    public void setSummaryReason(String summaryReason) {
        this.summaryReason = summaryReason;
    }

    public String getRejectedCandidatesJson() {
        return rejectedCandidatesJson;
    }

    public void setRejectedCandidatesJson(String rejectedCandidatesJson) {
        this.rejectedCandidatesJson = rejectedCandidatesJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OptimizationResult> getResults() {
        return results;
    }
}

package com.ps27.railway.entity;

import com.ps27.railway.enums.OptimizationDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Per-task outcome of an optimization run (Part 5 / plan part3). Stores the
 * deterministic decision (SCHEDULED / REJECTED), an explainable reason, the
 * candidate score, the persisted block reference when scheduled and the JSON
 * payload of all conflicts evaluated for that task.
 */
@Entity
@Table(name = "optimization_results", indexes = {
        @Index(name = "idx_optimization_results_run", columnList = "run_id")
})
public class OptimizationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private OptimizationRun run;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "task_code", nullable = false, length = 30)
    private String taskCode;

    @Column(name = "task_title", nullable = false, length = 150)
    private String taskTitle;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private OptimizationDecision decision;

    /** Human-readable, deterministic reason for the decision. */
    @Column(name = "reason", nullable = false, length = 2000)
    private String reason;

    /** Score of the best evaluated candidate; null when the task produced no candidate. */
    @Column(name = "score")
    private Integer score;

    /** Persisted block reference when SCHEDULED (null when REJECTED). */
    @Column(name = "block_id")
    private Long blockId;

    @Column(name = "block_code", length = 30)
    private String blockCode;

    @Column(name = "scheduled_start")
    private LocalDate scheduledStart;

    @Column(name = "scheduled_end")
    private LocalDate scheduledEnd;

    /** JSON array of all conflicts evaluated for this task (explainability). */
    @Column(name = "conflicts_json", columnDefinition = "TEXT")
    private String conflictsJson;

    protected OptimizationResult() {
        // JPA
    }

    public OptimizationResult(Long taskId, String taskCode, String taskTitle, String priority,
                              OptimizationDecision decision, String reason, Integer score,
                              Long blockId, String blockCode,
                              LocalDate scheduledStart, LocalDate scheduledEnd,
                              String conflictsJson) {
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.taskTitle = taskTitle;
        this.priority = priority;
        this.decision = decision;
        this.reason = reason;
        this.score = score;
        this.blockId = blockId;
        this.blockCode = blockCode;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.conflictsJson = conflictsJson;
    }

    public Long getId() {
        return id;
    }

    public OptimizationRun getRun() {
        return run;
    }

    public void setRun(OptimizationRun run) {
        this.run = run;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public String getPriority() {
        return priority;
    }

    public OptimizationDecision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public Integer getScore() {
        return score;
    }

    public Long getBlockId() {
        return blockId;
    }

    public String getBlockCode() {
        return blockCode;
    }

    public LocalDate getScheduledStart() {
        return scheduledStart;
    }

    public LocalDate getScheduledEnd() {
        return scheduledEnd;
    }

    public String getConflictsJson() {
        return conflictsJson;
    }
}

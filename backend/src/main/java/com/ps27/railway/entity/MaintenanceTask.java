package com.ps27.railway.entity;

import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Core maintenance requirement: what must be done, where, when (allowed window),
 * how long it takes and its priority. Later modules (conflict detection,
 * optimization) consume these fields.
 */
@Entity
@Table(name = "maintenance_tasks", indexes = {
        @Index(name = "idx_tasks_corridor_status", columnList = "corridor_id, status"),
        @Index(name = "idx_tasks_department", columnList = "department_id"),
        @Index(name = "idx_tasks_requested_start", columnList = "requested_start")
})
public class MaintenanceTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_code", nullable = false, unique = true, length = 30)
    private String taskCode;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority;

    /** Planned duration of the maintenance activity in minutes. */
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corridor_id", nullable = false)
    private Corridor corridor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Ideal/requested start of the task. */
    @Column(name = "requested_start")
    private LocalDate requestedStart;

    @Column(name = "requested_end")
    private LocalDate requestedEnd;

    /** Earliest start allowed by the operator (allowed time window start). */
    @Column(name = "earliest_start")
    private LocalDate earliestStart;

    /** Latest end allowed by the operator (allowed time window end). */
    @Column(name = "latest_end")
    private LocalDate latestEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status = TaskStatus.DRAFT;

    @Column(name = "notes", length = 1000)
    private String notes;

    /** Task dependencies (this task requires the listed tasks to be done first). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "maintenance_task_dependencies",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "depends_on_task_id"))
    private Set<MaintenanceTask> dependencies = new HashSet<>();

    protected MaintenanceTask() {
        // JPA
    }

    public MaintenanceTask(String taskCode, String title, String description, TaskType taskType,
                           Priority priority, int durationMinutes, Corridor corridor, Asset asset,
                           Department department, LocalDate requestedStart, LocalDate requestedEnd,
                           LocalDate earliestStart, LocalDate latestEnd, TaskStatus status, String notes) {
        this.taskCode = taskCode;
        this.title = title;
        this.description = description;
        this.taskType = taskType;
        this.priority = priority;
        this.durationMinutes = durationMinutes;
        this.corridor = corridor;
        this.asset = asset;
        this.department = department;
        this.requestedStart = requestedStart;
        this.requestedEnd = requestedEnd;
        this.earliestStart = earliestStart;
        this.latestEnd = latestEnd;
        this.status = status;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Corridor getCorridor() {
        return corridor;
    }

    public void setCorridor(Corridor corridor) {
        this.corridor = corridor;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public LocalDate getRequestedStart() {
        return requestedStart;
    }

    public void setRequestedStart(LocalDate requestedStart) {
        this.requestedStart = requestedStart;
    }

    public LocalDate getRequestedEnd() {
        return requestedEnd;
    }

    public void setRequestedEnd(LocalDate requestedEnd) {
        this.requestedEnd = requestedEnd;
    }

    public LocalDate getEarliestStart() {
        return earliestStart;
    }

    public void setEarliestStart(LocalDate earliestStart) {
        this.earliestStart = earliestStart;
    }

    public LocalDate getLatestEnd() {
        return latestEnd;
    }

    public void setLatestEnd(LocalDate latestEnd) {
        this.latestEnd = latestEnd;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Set<MaintenanceTask> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<MaintenanceTask> dependencies) {
        this.dependencies = dependencies;
    }
}

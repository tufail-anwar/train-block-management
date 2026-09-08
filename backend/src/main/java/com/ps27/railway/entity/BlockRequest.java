package com.ps27.railway.entity;

import com.ps27.railway.enums.BlockRequestStatus;
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
 * A maintenance block (traffic/line block) requested on a corridor to perform a
 * maintenance task. Later modules will evaluate these against train schedules.
 */
@Entity
@Table(name = "block_requests", indexes = {
        @Index(name = "idx_block_requests_corridor_status", columnList = "corridor_id, status"),
        @Index(name = "idx_block_requests_requested_start", columnList = "requested_start")
})
public class BlockRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "block_code", nullable = false, unique = true, length = 30)
    private String blockCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_task_id", nullable = false)
    private MaintenanceTask maintenanceTask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corridor_id", nullable = false)
    private Corridor corridor;

    @Column(name = "requested_start", nullable = false)
    private LocalDate requestedStart;

    @Column(name = "requested_end", nullable = false)
    private LocalDate requestedEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BlockRequestStatus status = BlockRequestStatus.DRAFT;

    @Column(name = "notes", length = 1000)
    private String notes;

    protected BlockRequest() {
        // JPA
    }

    public BlockRequest(String blockCode, MaintenanceTask maintenanceTask, Corridor corridor,
                        LocalDate requestedStart, LocalDate requestedEnd,
                        BlockRequestStatus status, String notes) {
        this.blockCode = blockCode;
        this.maintenanceTask = maintenanceTask;
        this.corridor = corridor;
        this.requestedStart = requestedStart;
        this.requestedEnd = requestedEnd;
        this.status = status;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public String getBlockCode() {
        return blockCode;
    }

    public void setBlockCode(String blockCode) {
        this.blockCode = blockCode;
    }

    public MaintenanceTask getMaintenanceTask() {
        return maintenanceTask;
    }

    public void setMaintenanceTask(MaintenanceTask maintenanceTask) {
        this.maintenanceTask = maintenanceTask;
    }

    public Corridor getCorridor() {
        return corridor;
    }

    public void setCorridor(Corridor corridor) {
        this.corridor = corridor;
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

    public BlockRequestStatus getStatus() {
        return status;
    }

    public void setStatus(BlockRequestStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

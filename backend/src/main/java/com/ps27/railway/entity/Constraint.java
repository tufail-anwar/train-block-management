package com.ps27.railway.entity;

import com.ps27.railway.enums.ConstraintType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A maintenance requirement/constraint (time window, availability rule, safety
 * rule, etc.). Stored in Part 1, interpreted later by conflict detection and
 * optimization. Optional task link; when null the constraint is global.
 */
@Entity
@Table(name = "constraints")
public class Constraint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "constraint_type", nullable = false, length = 30)
    private ConstraintType constraintType;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private MaintenanceTask task;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Constraint() {
        // JPA
    }

    public Constraint(ConstraintType constraintType, String name, String description,
                      MaintenanceTask task, boolean active) {
        this.constraintType = constraintType;
        this.name = name;
        this.description = description;
        this.task = task;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public ConstraintType getConstraintType() {
        return constraintType;
    }

    public void setConstraintType(ConstraintType constraintType) {
        this.constraintType = constraintType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MaintenanceTask getTask() {
        return task;
    }

    public void setTask(MaintenanceTask task) {
        this.task = task;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

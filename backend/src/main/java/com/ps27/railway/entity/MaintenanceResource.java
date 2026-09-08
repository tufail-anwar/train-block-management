package com.ps27.railway.entity;

import com.ps27.railway.enums.ResourceType;
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

/** Crew or equipment available for maintenance. Resource capacity is an optimization input. */
@Entity
@Table(name = "maintenance_resources")
public class MaintenanceResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private ResourceType resourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Number of units/people available per shift. */
    @Column(name = "capacity_per_shift", nullable = false)
    private int capacityPerShift = 1;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected MaintenanceResource() {
        // JPA
    }

    public MaintenanceResource(String code, String name, ResourceType resourceType,
                               Department department, int capacityPerShift, boolean active) {
        this.code = code;
        this.name = name;
        this.resourceType = resourceType;
        this.department = department;
        this.capacityPerShift = capacityPerShift;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public int getCapacityPerShift() {
        return capacityPerShift;
    }

    public void setCapacityPerShift(int capacityPerShift) {
        this.capacityPerShift = capacityPerShift;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

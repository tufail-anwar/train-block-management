package com.ps27.railway.entity;

import com.ps27.railway.enums.CorridorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Railway corridor/route on which trains run and maintenance is performed. */
@Entity
@Table(name = "corridors")
public class Corridor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_location", nullable = false, length = 100)
    private String startLocation;

    @Column(name = "end_location", nullable = false, length = 100)
    private String endLocation;

    @Column(name = "length_km", nullable = false)
    private double lengthKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CorridorStatus status = CorridorStatus.OPERATIONAL;

    protected Corridor() {
        // JPA
    }

    public Corridor(String code, String name, String startLocation, String endLocation,
                    double lengthKm, CorridorStatus status) {
        this.code = code;
        this.name = name;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.lengthKm = lengthKm;
        this.status = status;
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

    public String getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(String startLocation) {
        this.startLocation = startLocation;
    }

    public String getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(String endLocation) {
        this.endLocation = endLocation;
    }

    public double getLengthKm() {
        return lengthKm;
    }

    public void setLengthKm(double lengthKm) {
        this.lengthKm = lengthKm;
    }

    public CorridorStatus getStatus() {
        return status;
    }

    public void setStatus(CorridorStatus status) {
        this.status = status;
    }
}

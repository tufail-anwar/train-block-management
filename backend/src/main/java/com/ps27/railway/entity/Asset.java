package com.ps27.railway.entity;

import com.ps27.railway.enums.AssetStatus;
import com.ps27.railway.enums.AssetType;
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

/** A physical railway asset (track section, signal, bridge, etc.) maintained on a corridor. */
@Entity
@Table(name = "assets", indexes = {
        @Index(name = "idx_assets_corridor", columnList = "corridor_id")
})
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    private AssetType assetType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corridor_id", nullable = false)
    private Corridor corridor;

    @Column(name = "track_km")
    private Double trackKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AssetStatus status = AssetStatus.OPERATIONAL;

    protected Asset() {
        // JPA
    }

    public Asset(String code, String name, AssetType assetType, Corridor corridor,
                 Double trackKm, AssetStatus status) {
        this.code = code;
        this.name = name;
        this.assetType = assetType;
        this.corridor = corridor;
        this.trackKm = trackKm;
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

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public Corridor getCorridor() {
        return corridor;
    }

    public void setCorridor(Corridor corridor) {
        this.corridor = corridor;
    }

    public Double getTrackKm() {
        return trackKm;
    }

    public void setTrackKm(Double trackKm) {
        this.trackKm = trackKm;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }
}

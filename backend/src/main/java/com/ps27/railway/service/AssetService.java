package com.ps27.railway.service;

import com.ps27.railway.dto.AssetRequest;
import com.ps27.railway.dto.AssetResponse;
import com.ps27.railway.entity.Asset;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.enums.AssetStatus;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.AssetRepository;
import com.ps27.railway.repository.CorridorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssetService {

    private final AssetRepository repository;
    private final CorridorRepository corridorRepository;

    public AssetService(AssetRepository repository, CorridorRepository corridorRepository) {
        this.repository = repository;
        this.corridorRepository = corridorRepository;
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AssetResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new BadRequestException("Asset code already exists: " + request.code());
        }
        Corridor corridor = corridor(request.corridorId());
        Asset entity = new Asset(request.code(), request.name(), request.assetType(), corridor,
                request.trackKm(),
                request.status() != null ? request.status() : AssetStatus.OPERATIONAL);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public AssetResponse update(Long id, AssetRequest request) {
        Asset entity = getEntity(id);
        repository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BadRequestException("Asset code already exists: " + request.code());
                });
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setAssetType(request.assetType());
        entity.setCorridor(corridor(request.corridorId()));
        entity.setTrackKm(request.trackKm());
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        Asset entity = getEntity(id);
        repository.delete(entity);
    }

    private Asset getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
    }

    private Corridor corridor(Long id) {
        return corridorRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Corridor not found: " + id));
    }

    private AssetResponse toResponse(Asset a) {
        return new AssetResponse(a.getId(), a.getCode(), a.getName(), a.getAssetType(),
                a.getCorridor().getId(), a.getCorridor().getCode(), a.getTrackKm(), a.getStatus());
    }
}

package com.ps27.railway.repository;

import com.ps27.railway.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByCode(String code);

    boolean existsByCode(String code);

    List<Asset> findByCorridorId(Long corridorId);
}

package com.ps27.railway.repository;

import com.ps27.railway.entity.BlockRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BlockRequestRepository extends JpaRepository<BlockRequest, Long> {

    Optional<BlockRequest> findByBlockCode(String blockCode);

    boolean existsByBlockCode(String blockCode);

    List<BlockRequest> findByCorridorId(Long corridorId);

    List<BlockRequest> findByCorridorIdAndRequestedStartBetween(Long corridorId,
                                                                LocalDate from,
                                                                LocalDate to);
}

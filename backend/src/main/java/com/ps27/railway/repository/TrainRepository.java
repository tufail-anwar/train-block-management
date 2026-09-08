package com.ps27.railway.repository;

import com.ps27.railway.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {

    Optional<Train> findByCode(String code);

    boolean existsByCode(String code);
}

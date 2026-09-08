package com.ps27.railway.repository;

import com.ps27.railway.entity.Corridor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorridorRepository extends JpaRepository<Corridor, Long> {

    Optional<Corridor> findByCode(String code);

    boolean existsByCode(String code);
}

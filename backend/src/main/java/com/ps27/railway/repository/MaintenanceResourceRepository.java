package com.ps27.railway.repository;

import com.ps27.railway.entity.MaintenanceResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaintenanceResourceRepository extends JpaRepository<MaintenanceResource, Long> {

    Optional<MaintenanceResource> findByCode(String code);

    boolean existsByCode(String code);
}

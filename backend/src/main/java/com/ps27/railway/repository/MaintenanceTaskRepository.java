package com.ps27.railway.repository;

import com.ps27.railway.entity.MaintenanceTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> {

    Optional<MaintenanceTask> findByTaskCode(String taskCode);

    boolean existsByTaskCode(String taskCode);

    List<MaintenanceTask> findByCorridorIdAndStatusIn(Long corridorId,
                                                      List<com.ps27.railway.enums.TaskStatus> statuses);

    List<MaintenanceTask> findByRequestedStartBetween(
            java.time.LocalDate from, java.time.LocalDate to);
}

package com.ps27.railway.repository;

import com.ps27.railway.entity.TrainSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TrainScheduleRepository extends JpaRepository<TrainSchedule, Long> {

    List<TrainSchedule> findByCorridorId(Long corridorId);

    List<TrainSchedule> findByCorridorIdAndScheduleDateBetween(Long corridorId,
                                                               LocalDate from,
                                                               LocalDate to);
}

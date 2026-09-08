package com.ps27.railway.repository;

import com.ps27.railway.entity.Constraint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConstraintRepository extends JpaRepository<Constraint, Long> {

    List<Constraint> findByTaskId(Long taskId);

    List<Constraint> findByActiveTrue();
}

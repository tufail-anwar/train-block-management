package com.ps27.railway.repository;

import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.Department;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MaintenanceTaskRepositoryTest {

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    @Autowired
    private CorridorRepository corridorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void findByTaskCodeReturnsTask() {
        Corridor corridor = corridorRepository.save(
                new Corridor("COR-T1", "Test Corridor", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
        MaintenanceTask task = taskRepository.save(task("TASK-T1", corridor, null));

        assertThat(taskRepository.findByTaskCode("TASK-T1")).isPresent();
        assertThat(taskRepository.existsByTaskCode("TASK-T1")).isTrue();
    }

    @Test
    void findByRequestedStartBetweenReturnsMatchingTasks() {
        Corridor corridor = corridorRepository.save(
                new Corridor("COR-T2", "Test Corridor 2", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        taskRepository.save(task("TASK-T2-IN", corridor, from.plusDays(5)));
        taskRepository.save(task("TASK-T3-OUT", corridor, from.minusDays(10)));

        List<MaintenanceTask> result = taskRepository.findByRequestedStartBetween(from, to);
        assertThat(result).extracting(MaintenanceTask::getTaskCode)
                .containsExactly("TASK-T2-IN");
    }

    @Test
    void findByCorridorIdAndStatusInReturnsMatchingTasks() {
        Corridor corridor = corridorRepository.save(
                new Corridor("COR-T3", "Test Corridor 3", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
        MaintenanceTask requested = task("TASK-T4", corridor, null);
        requested.setStatus(TaskStatus.REQUESTED);
        taskRepository.save(requested);
        MaintenanceTask draft = task("TASK-T5", corridor, null);
        taskRepository.save(draft);

        List<MaintenanceTask> result = taskRepository.findByCorridorIdAndStatusIn(
                corridor.getId(), List.of(TaskStatus.REQUESTED));
        assertThat(result).extracting(MaintenanceTask::getTaskCode)
                .containsExactly("TASK-T4");
    }

    private MaintenanceTask task(String code, Corridor corridor, LocalDate requestedStart) {
        return new MaintenanceTask(code, "Test task " + code, "desc", TaskType.INSPECTION,
                Priority.MEDIUM, 120, corridor, null, null,
                requestedStart, requestedStart, null, null, TaskStatus.DRAFT, null);
    }
}

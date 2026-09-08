package com.ps27.railway.service;

import com.ps27.railway.dto.MaintenanceTaskRequest;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MaintenanceTaskServiceTest {

    @Autowired
    private MaintenanceTaskService service;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    @Autowired
    private CorridorRepository corridorRepository;

    @Test
    void createAndReadTask() {
        Corridor corridor = corridorRepository.save(
                new Corridor("COR-SVC", "Svc Corridor", "A", "B", 10.0, CorridorStatus.OPERATIONAL));

        MaintenanceTaskRequest request = new MaintenanceTaskRequest(
                "TASK-SVC-1", "Test task", "desc", TaskType.INSPECTION, Priority.HIGH, 240,
                corridor.getId(), null, null,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), null, null,
                TaskStatus.REQUESTED, null, null);

        var created = service.create(request);
        assertThat(created.id()).isNotNull();
        assertThat(created.taskCode()).isEqualTo("TASK-SVC-1");
        assertThat(created.status()).isEqualTo(TaskStatus.REQUESTED);

        var found = service.findById(created.id());
        assertThat(found.title()).isEqualTo("Test task");
        assertThat(found.corridorId()).isEqualTo(corridor.getId());
    }

    @Test
    void createRejectsInvalidDateRange() {
        Corridor corridor = corridorRepository.save(
                new Corridor("COR-SVC2", "Svc Corridor 2", "A", "B", 10.0, CorridorStatus.OPERATIONAL));

        MaintenanceTaskRequest request = new MaintenanceTaskRequest(
                "TASK-SVC-2", "Bad dates", "desc", TaskType.INSPECTION, Priority.LOW, 60,
                corridor.getId(), null, null,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), null, null,
                null, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requestedEnd must be after");
    }

    @Test
    void updateRejectsSelfDependency() {
        Corridor corridor = corridorRepository.save(
                new Corridor("COR-SVC3", "Svc Corridor 3", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
        MaintenanceTask existing = taskRepository.save(new MaintenanceTask(
                "TASK-SVC-3", "Existing", "desc", TaskType.INSPECTION, Priority.LOW, 60,
                corridor, null, null, null, null, null, null, TaskStatus.DRAFT, null));

        MaintenanceTaskRequest request = new MaintenanceTaskRequest(
                "TASK-SVC-3", "Depends on self", "desc", TaskType.INSPECTION, Priority.LOW, 60,
                corridor.getId(), null, null, null, null, null, null,
                null, null, Set.of(existing.getId()));

        assertThatThrownBy(() -> service.update(existing.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot depend on itself");
    }

    @Test
    void deleteMissingTaskThrowsNotFound() {
        assertThatThrownBy(() -> service.delete(999999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

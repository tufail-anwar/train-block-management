package com.ps27.railway.service;

import com.ps27.railway.dto.OptimizationResultResponse;
import com.ps27.railway.dto.OptimizationRunDetailResponse;
import com.ps27.railway.dto.OptimizationRunRequest;
import com.ps27.railway.dto.OptimizationRunRequest.BlackoutWindow;
import com.ps27.railway.entity.BlockRequest;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.entity.Train;
import com.ps27.railway.entity.TrainSchedule;
import com.ps27.railway.enums.BlockRequestStatus;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.enums.Direction;
import com.ps27.railway.enums.OptimizationDecision;
import com.ps27.railway.enums.OptimizationStatus;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
import com.ps27.railway.enums.TrainType;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.BlockRequestRepository;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import com.ps27.railway.repository.OptimizationRunRepository;
import com.ps27.railway.repository.TrainRepository;
import com.ps27.railway.repository.TrainScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Deterministic block optimizer tests (Part 5). All windows use far-future dates
 * so the synthetic sample data (seeded around "now") never interferes, and each
 * run targets an explicit {@code taskIds} list for deterministic assertions.
 */
@SpringBootTest
@Transactional
class OptimizationServiceTest {

    /** A fixed far-future Tuesday, far away from the seeded sample data. */
    private static final LocalDate D = LocalDate.of(2030, 6, 4);

    @Autowired
    private OptimizationService service;

    @Autowired
    private OptimizationRunRepository runRepository;

    @Autowired
    private CorridorRepository corridorRepository;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    @Autowired
    private BlockRequestRepository blockRequestRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainScheduleRepository trainScheduleRepository;

    // --- helpers ----------------------------------------------------------

    private Corridor corridor(String code) {
        return corridorRepository.save(
                new Corridor(code, code + " Corridor", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
    }

    /** Task with a single-day window {@code [reqStart, reqEnd]} and no department (avoids resource checks). */
    private MaintenanceTask task(String code, Corridor c, Priority priority, int duration,
                                 LocalDate reqStart, LocalDate reqEnd) {
        return taskRepository.save(new MaintenanceTask(code, code + " title", "desc", TaskType.INSPECTION,
                priority, duration, c, null, null, reqStart, reqEnd, reqStart, reqEnd,
                TaskStatus.REQUESTED, null));
    }

    private OptimizationRunRequest request(LocalDate from, LocalDate to, List<Long> taskIds) {
        return new OptimizationRunRequest(from, to, taskIds, null, null, null);
    }

    // --- feasible candidate ----------------------------------------------

    @Test
    void feasibleTaskIsScheduledOnEarliestStart() {
        Corridor c = corridor("COR-OPT1");
        MaintenanceTask t = task("T1-OPT", c, Priority.LOW, 120, D, D.plusDays(3));

        OptimizationRunDetailResponse run = service.run(request(D, D.plusDays(3), List.of(t.getId())));

        assertThat(run.status()).isEqualTo(OptimizationStatus.COMPLETED);
        assertThat(run.tasksScheduled()).isEqualTo(1);
        assertThat(run.tasksRejected()).isZero();
        OptimizationResultResponse result = run.results().get(0);
        assertThat(result.decision()).isEqualTo(OptimizationDecision.SCHEDULED);
        assertThat(result.scheduledStart()).isEqualTo(D);
        // priorityWeight(LOW)=10 + 5 * daysEarly(3) = 25
        assertThat(result.score()).isEqualTo(25);
        assertThat(result.blockCode()).startsWith("OPT-T1-OPT-");
        // Feasible candidate was persisted as a SCHEDULED block request.
        assertThat(blockRequestRepository.findByBlockCode(result.blockCode())).isPresent();
    }

    // --- train overlap ----------------------------------------------------

    @Test
    void trainOverlapTaskIsRejectedWithExplanation() {
        Corridor c = corridor("COR-OPT2");
        // Passenger trains on every candidate day -> TRAIN_OVERLAP (HIGH) for all.
        Train train = trainRepository.save(new Train("TRN-OPT2", "Passenger", TrainType.PASSENGER, true));
        for (int i = 0; i <= 2; i++) {
            trainScheduleRepository.save(new TrainSchedule(train, c, D.plusDays(i), null,
                    LocalTime.of(6, 0), LocalTime.of(8, 0), Direction.UP));
        }
        MaintenanceTask t = task("T2-OPT", c, Priority.MEDIUM, 120, D, D.plusDays(2));

        OptimizationRunDetailResponse run = service.run(request(D, D.plusDays(2), List.of(t.getId())));

        assertThat(run.tasksRejected()).isEqualTo(1);
        OptimizationResultResponse result = run.results().get(0);
        assertThat(result.decision()).isEqualTo(OptimizationDecision.REJECTED);
        assertThat(result.reason()).contains("TRAIN_OVERLAP");
    }

    // --- max concurrent blocks -------------------------------------------

    @Test
    void maxConcurrentBlocksCapsScheduling() {
        Corridor c1 = corridor("COR-OPT3A");
        Corridor c2 = corridor("COR-OPT3B");
        // Identical single-day windows on different corridors; only one block may run per day.
        MaintenanceTask high = task("T3A-OPT", c1, Priority.HIGH, 60, D, D);
        MaintenanceTask low = task("T3B-OPT", c2, Priority.LOW, 60, D, D);

        OptimizationRunRequest req = new OptimizationRunRequest(D, D, List.of(high.getId(), low.getId()),
                1, null, null);
        OptimizationRunDetailResponse run = service.run(req);

        // HIGH is ranked first and takes the single day slot.
        OptimizationResultResponse a = resultFor(run, high.getId());
        OptimizationResultResponse b = resultFor(run, low.getId());
        assertThat(a.decision()).isEqualTo(OptimizationDecision.SCHEDULED);
        assertThat(b.decision()).isEqualTo(OptimizationDecision.REJECTED);
        assertThat(b.reason()).contains("maximum concurrent blocks");
    }

    // --- blackout windows -------------------------------------------------

    @Test
    void blackoutWindowRejectsOverlappingCandidates() {
        Corridor c = corridor("COR-OPT4");
        MaintenanceTask t = task("T4-OPT", c, Priority.HIGH, 120, D, D.plusDays(4));

        OptimizationRunRequest req = new OptimizationRunRequest(D, D.plusDays(4), List.of(t.getId()),
                null, List.of(new BlackoutWindow(D.plusDays(1), D.plusDays(3))), null);
        OptimizationRunDetailResponse run = service.run(req);

        // D and D+4 remain feasible; earliest start D wins the score.
        OptimizationResultResponse result = resultFor(run, t.getId());
        assertThat(result.decision()).isEqualTo(OptimizationDecision.SCHEDULED);
        assertThat(result.scheduledStart()).isEqualTo(D);
        assertThat(run.rejectedCandidates()).anyMatch(s -> s.contains("blackout"));
    }

    // --- task already planned --------------------------------------------

    @Test
    void taskWithExistingActiveBlockIsSkipped() {
        Corridor c = corridor("COR-OPT5");
        MaintenanceTask t = task("T5-OPT", c, Priority.HIGH, 120, D, D.plusDays(2));
        blockRequestRepository.save(new BlockRequest("BLK-OPT5", t, c, D, D, BlockRequestStatus.REQUESTED, null));

        OptimizationRunDetailResponse run = service.run(request(D, D.plusDays(5), List.of(t.getId())));

        assertThat(run.tasksRejected()).isEqualTo(1);
        assertThat(resultFor(run, t.getId()).reason()).contains("already has an active block");
    }

    // --- priority ranking -------------------------------------------------

    @Test
    void criticalTaskIsScheduledBeforeLowerPriority() {
        Corridor c1 = corridor("COR-OPT6A");
        Corridor c2 = corridor("COR-OPT6B");
        MaintenanceTask critical = task("T6A-OPT", c1, Priority.CRITICAL, 60, D, D);
        MaintenanceTask low = task("T6B-OPT", c2, Priority.LOW, 60, D, D);

        OptimizationRunRequest req = new OptimizationRunRequest(D, D, List.of(critical.getId(), low.getId()),
                1, null, null);
        OptimizationRunDetailResponse run = service.run(req);

        assertThat(resultFor(run, critical.getId()).decision()).isEqualTo(OptimizationDecision.SCHEDULED);
        assertThat(resultFor(run, low.getId()).decision()).isEqualTo(OptimizationDecision.REJECTED);
    }

    // --- determinism ------------------------------------------------------

    @Test
    void repeatedRunProducesIdenticalPlan() {
        Corridor c = corridor("COR-OPT7");
        MaintenanceTask t = task("T7-OPT", c, Priority.MEDIUM, 240, D, D.plusDays(6));

        // persistBlocks=false so the second run sees exactly the same state.
        OptimizationRunRequest req = new OptimizationRunRequest(D, D.plusDays(6), List.of(t.getId()),
                null, null, false);
        OptimizationRunDetailResponse first = service.run(req);
        OptimizationRunDetailResponse second = service.run(req);

        assertThat(second.objectiveScore()).isEqualTo(first.objectiveScore());
        assertThat(second.tasksScheduled()).isEqualTo(first.tasksScheduled());
        assertThat(second.results().get(0).scheduledStart())
                .isEqualTo(first.results().get(0).scheduledStart());
    }

    // --- validation + 404 -------------------------------------------------

    @Test
    void reversedWindowThrowsBadRequest() {
        assertThatThrownBy(() -> service.run(request(D.plusDays(2), D, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("windowEnd");
    }

    @Test
    void unknownTaskIdThrowsBadRequest() {
        assertThatThrownBy(() -> service.run(request(D, D.plusDays(1), List.of(999999L))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown task");
    }

    @Test
    void missingRunThrowsNotFound() {
        assertThatThrownBy(() -> service.getRun(999999L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getResults(999999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- helpers ----------------------------------------------------------

    private OptimizationResultResponse resultFor(OptimizationRunDetailResponse run, Long taskId) {
        return run.results().stream()
                .filter(r -> r.taskId().equals(taskId))
                .findFirst()
                .orElseThrow();
    }
}

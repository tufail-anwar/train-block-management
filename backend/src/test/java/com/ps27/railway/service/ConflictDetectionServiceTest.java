package com.ps27.railway.service;

import com.ps27.railway.dto.BlockRequestRequest;
import com.ps27.railway.dto.ConflictCheckResponse;
import com.ps27.railway.dto.ConflictResponse;
import com.ps27.railway.entity.Asset;
import com.ps27.railway.entity.BlockRequest;
import com.ps27.railway.entity.Constraint;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.Department;
import com.ps27.railway.entity.MaintenanceResource;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.entity.Train;
import com.ps27.railway.entity.TrainSchedule;
import com.ps27.railway.enums.AssetStatus;
import com.ps27.railway.enums.AssetType;
import com.ps27.railway.enums.BlockRequestStatus;
import com.ps27.railway.enums.ConflictSeverity;
import com.ps27.railway.enums.ConflictType;
import com.ps27.railway.enums.ConstraintType;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.enums.Direction;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.ResourceType;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
import com.ps27.railway.enums.TrainType;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.AssetRepository;
import com.ps27.railway.repository.BlockRequestRepository;
import com.ps27.railway.repository.ConstraintRepository;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.DepartmentRepository;
import com.ps27.railway.repository.MaintenanceResourceRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import com.ps27.railway.repository.TrainRepository;
import com.ps27.railway.repository.TrainScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ConflictDetectionServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 12, 1); // a Tuesday

    @Autowired
    private ConflictDetectionService service;

    @Autowired
    private CorridorRepository corridorRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MaintenanceResourceRepository resourceRepository;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainScheduleRepository trainScheduleRepository;

    @Autowired
    private BlockRequestRepository blockRequestRepository;

    @Autowired
    private ConstraintRepository constraintRepository;

    // --- helpers ----------------------------------------------------------

    private Corridor corridor(String code) {
        return corridorRepository.save(
                new Corridor(code, code + " Corridor", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
    }

    private Department department(String code) {
        return departmentRepository.save(new Department(code, code + " Dept", "desc"));
    }

    private Asset asset(Corridor c, String code) {
        return assetRepository.save(
                new Asset(code, code + " asset", AssetType.TRACK_SECTION, c, 1.0, AssetStatus.OPERATIONAL));
    }

    private MaintenanceTask task(String code, Corridor c, Department dept, Asset asset,
                                 int duration, Priority priority, TaskStatus status) {
        return taskRepository.save(new MaintenanceTask(code, code + " title", "desc", TaskType.INSPECTION,
                priority, duration, c, asset, dept, null, null, null, null, status, null));
    }

    private BlockRequest block(String code, MaintenanceTask task, Corridor c,
                               LocalDate start, LocalDate end, BlockRequestStatus status) {
        return blockRequestRepository.save(new BlockRequest(code, task, c, start, end, status, null));
    }

    // --- TRAIN_OVERLAP ----------------------------------------------------

    @Test
    void detectsTrainOverlapHighForPassengerTrain() {
        Corridor c = corridor("COR-TRN");
        Train train = trainRepository.save(new Train("TRN-1", "Passenger Express", TrainType.PASSENGER, true));
        trainScheduleRepository.save(new TrainSchedule(train, c, D, null,
                LocalTime.of(6, 0), LocalTime.of(8, 0), Direction.UP));
        MaintenanceTask t = task("TASK-TRN", c, null, null, 120, Priority.MEDIUM, TaskStatus.REQUESTED);
        block("BLK-TRN", t, c, D, D, BlockRequestStatus.REQUESTED);

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.TRAIN_OVERLAP
                        && x.severity() == ConflictSeverity.HIGH
                        && x.blockCode().equals("BLK-TRN")
                        && x.explanation().contains("TRN-1"));
    }

    @Test
    void trainOverlapIsMediumForFreightTrain() {
        Corridor c = corridor("COR-FRT");
        Train train = trainRepository.save(new Train("TRN-F", "Freight", TrainType.FREIGHT, true));
        trainScheduleRepository.save(new TrainSchedule(train, c, D, null,
                LocalTime.of(22, 0), LocalTime.of(23, 0), Direction.DOWN));
        MaintenanceTask t = task("TASK-FRT", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        block("BLK-FRT", t, c, D, D, BlockRequestStatus.REQUESTED);

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.TRAIN_OVERLAP
                        && x.severity() == ConflictSeverity.MEDIUM);
    }

    @Test
    void recurringScheduleOverlapsBlockOnMatchingWeekday() {
        Corridor c = corridor("COR-REC");
        Train train = trainRepository.save(new Train("TRN-R", "Recurring", TrainType.SUBURBAN, true));
        // Recurring every TUESDAY; D (2026-12-01) is a Tuesday and lies in the block range.
        trainScheduleRepository.save(new TrainSchedule(train, c, null, DayOfWeek.TUESDAY,
                LocalTime.of(7, 0), LocalTime.of(7, 30), Direction.UP));
        MaintenanceTask t = task("TASK-REC", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        block("BLK-REC", t, c, D, D.plusDays(2), BlockRequestStatus.REQUESTED);

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.TRAIN_OVERLAP
                        && x.date().equals(D));
    }

    // --- BLOCK_OVERLAP ----------------------------------------------------

    @Test
    void detectsBlockOverlapOnSameCorridor() {
        Corridor c = corridor("COR-OVL");
        MaintenanceTask t1 = task("T1-OVL", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        MaintenanceTask t2 = task("T2-OVL", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        block("BLK-A", t1, c, D, D.plusDays(2), BlockRequestStatus.REQUESTED);
        block("BLK-B", t2, c, D.plusDays(1), D.plusDays(3), BlockRequestStatus.REQUESTED);

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.BLOCK_OVERLAP
                        && x.details().contains("BLK-B")
                        && x.blockCode().equals("BLK-A"));
    }

    // --- SPATIAL_OVERLAP --------------------------------------------------

    @Test
    void detectsSpatialOverlapOnSameAsset() {
        Corridor c = corridor("COR-SPA");
        Asset a = asset(c, "AST-SPA");
        MaintenanceTask t1 = task("T1-SPA", c, null, a, 60, Priority.LOW, TaskStatus.REQUESTED);
        MaintenanceTask t2 = task("T2-SPA", c, null, a, 60, Priority.LOW, TaskStatus.REQUESTED);
        block("BLK-S1", t1, c, D, D.plusDays(2), BlockRequestStatus.REQUESTED);
        block("BLK-S2", t2, c, D.plusDays(1), D.plusDays(1), BlockRequestStatus.REQUESTED);

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.SPATIAL_OVERLAP
                        && x.details().contains("AST-SPA"));
    }

    // --- INSUFFICIENT_GAP -------------------------------------------------

    @Test
    void detectsInsufficientGapBetweenConsecutiveBlocks() {
        Corridor c = corridor("COR-GAP");
        MaintenanceTask t1 = task("T1-GAP", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        MaintenanceTask t2 = task("T2-GAP", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        block("BLK-G1", t1, c, D, D, BlockRequestStatus.REQUESTED);
        block("BLK-G2", t2, c, D.plusDays(1), D.plusDays(1), BlockRequestStatus.REQUESTED); // 0 free days

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.INSUFFICIENT_GAP
                        && x.severity() == ConflictSeverity.LOW
                        && x.explanation().contains("minimum gap"));
    }

    // --- RESOURCE_CONFLICT ------------------------------------------------

    @Test
    void detectsResourceCapacityConflict() {
        Corridor c = corridor("COR-RES");
        Department dept = department("DEPT-RES");
        resourceRepository.save(new MaintenanceResource("RES-1", "Crew", ResourceType.CREW,
                dept, 1, true)); // capacity 1
        MaintenanceTask t1 = task("T1-RES", c, dept, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        MaintenanceTask t2 = task("T2-RES", c, dept, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        block("BLK-R1", t1, c, D, D, BlockRequestStatus.REQUESTED);
        block("BLK-R2", t2, c, D, D, BlockRequestStatus.REQUESTED); // 2 needed on same day > capacity 1

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.RESOURCE_CONFLICT
                        && x.severity() == ConflictSeverity.MEDIUM
                        && x.explanation().contains("DEPT-RES"));
    }

    // --- CONSTRAINT_VIOLATION ---------------------------------------------

    @Test
    void detectsWindowViolationBeforeEarliestStart() {
        Corridor c = corridor("COR-WIN");
        MaintenanceTask t = new MaintenanceTask("T-WIN", "Window task", "desc", TaskType.INSPECTION,
                Priority.HIGH, 120, c, null, null, null, null,
                D.plusDays(2), D.plusDays(5), TaskStatus.REQUESTED, null);
        taskRepository.save(t);
        block("BLK-WIN", t, c, D, D.plusDays(1), BlockRequestStatus.REQUESTED); // starts before earliest

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.CONSTRAINT_VIOLATION
                        && x.severity() == ConflictSeverity.HIGH
                        && x.explanation().contains("earliest"));
    }

    @Test
    void detectsUnmetDependency() {
        Corridor c = corridor("COR-DEP");
        MaintenanceTask dependency = task("DEP-1", c, null, null, 60, Priority.LOW, TaskStatus.DRAFT);
        MaintenanceTask t = task("T-DEP", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        // Use a mutable set - Hibernate mutates the collection when flushing the join table.
        t.setDependencies(new HashSet<>(Set.of(dependency)));
        taskRepository.save(t);
        block("BLK-DEP", t, c, D, D, BlockRequestStatus.REQUESTED);

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.CONSTRAINT_VIOLATION
                        && x.severity() == ConflictSeverity.MEDIUM
                        && x.explanation().contains("DEP-1"));
    }

    @Test
    void detectsActiveSafetyRuleViolation() {
        Corridor c = corridor("COR-SAF");
        MaintenanceTask t = task("T-SAF", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        constraintRepository.save(new Constraint(ConstraintType.SAFETY_RULE, "No night work",
                "Safety rule", t, true));
        block("BLK-SAF", t, c, D, D, BlockRequestStatus.REQUESTED);

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.CONSTRAINT_VIOLATION
                        && x.severity() == ConflictSeverity.HIGH
                        && x.explanation().contains("No night work"));
    }

    // --- DURATION_FIT -----------------------------------------------------

    @Test
    void detectsDurationFitExceedingDailyLimit() {
        Corridor c = corridor("COR-DUR");
        MaintenanceTask t = task("T-DUR", c, null, null, 900, Priority.HIGH, TaskStatus.REQUESTED);
        block("BLK-DUR", t, c, D, D, BlockRequestStatus.REQUESTED); // single day, 900 > 720

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.DURATION_FIT
                        && x.severity() == ConflictSeverity.HIGH);
    }

    @Test
    void multiDayBlockDoesNotTriggerDurationFit() {
        Corridor c = corridor("COR-DUR2");
        MaintenanceTask t = task("T-DUR2", c, null, null, 900, Priority.HIGH, TaskStatus.REQUESTED);
        block("BLK-DUR2", t, c, D, D.plusDays(1), BlockRequestStatus.REQUESTED); // two days

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .noneMatch(x -> x.type() == ConflictType.DURATION_FIT);
    }

    // --- what-if check ----------------------------------------------------

    @Test
    void whatIfCleanBlockReturnsNoConflicts() {
        Corridor c = corridor("COR-CLEAN");
        MaintenanceTask t = new MaintenanceTask("T-CLEAN", "Clean task", "desc", TaskType.INSPECTION,
                Priority.LOW, 120, c, null, null, null, null,
                D, D.plusDays(10), TaskStatus.REQUESTED, null);
        taskRepository.save(t);

        BlockRequestRequest request = new BlockRequestRequest(
                "BLK-WHATIF", t.getId(), c.getId(), D.plusDays(3), D.plusDays(4),
                BlockRequestStatus.REQUESTED, null);

        ConflictCheckResponse result = service.checkProposed(request);

        assertThat(result.blockCode()).isEqualTo("BLK-WHATIF");
        assertThat(result.conflicts()).isEmpty();
    }

    @Test
    void whatIfConflictingProposalReturnsConflicts() {
        Corridor c = corridor("COR-CONFLICT");
        MaintenanceTask t = task("T-CONFLICT", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        block("BLK-EXISTING", t, c, D, D.plusDays(3), BlockRequestStatus.REQUESTED);

        BlockRequestRequest request = new BlockRequestRequest(
                "BLK-PROPOSED", t.getId(), c.getId(), D.plusDays(1), D.plusDays(2),
                BlockRequestStatus.REQUESTED, null);

        ConflictCheckResponse result = service.checkProposed(request);

        assertThat(result.conflicts())
                .anyMatch(x -> x.type() == ConflictType.BLOCK_OVERLAP);
    }

    // --- detection scope / errors ------------------------------------------

    @Test
    void detectForBlockFindsConflicts() {
        Corridor c = corridor("COR-BYID");
        MaintenanceTask t1 = task("T1-BYID", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        MaintenanceTask t2 = task("T2-BYID", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        BlockRequest a = block("BLK-A1", t1, c, D, D.plusDays(2), BlockRequestStatus.REQUESTED);
        block("BLK-B1", t2, c, D.plusDays(1), D.plusDays(3), BlockRequestStatus.REQUESTED);

        List<ConflictResponse> conflicts = service.detectForBlock(a.getId());

        assertThat(conflicts)
                .anyMatch(x -> x.type() == ConflictType.BLOCK_OVERLAP
                        && x.details().contains("BLK-B1"));
    }

    @Test
    void detectForMissingBlockThrowsNotFound() {
        assertThatThrownBy(() -> service.detectForBlock(999999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void corridorFilterOnlyIncludesThatCorridor() {
        Corridor c1 = corridor("COR-F1");
        Corridor c2 = corridor("COR-F2");
        MaintenanceTask t1 = task("T1-F", c1, null, null, 900, Priority.HIGH, TaskStatus.REQUESTED);
        MaintenanceTask t2 = task("T2-F", c2, null, null, 900, Priority.HIGH, TaskStatus.REQUESTED);
        block("BLK-F1", t1, c1, D, D, BlockRequestStatus.REQUESTED);
        block("BLK-F2", t2, c2, D, D, BlockRequestStatus.REQUESTED);

        List<ConflictResponse> conflicts = service.detectAll(c1.getId(), null, null);

        assertThat(conflicts)
                .allMatch(x -> x.corridorCode().equals("COR-F1"))
                .anyMatch(x -> x.type() == ConflictType.DURATION_FIT);
    }

    @Test
    void dateRangeFilterNarrowsDetection() {
        Corridor c = corridor("COR-DATE");
        MaintenanceTask t1 = task("T1-DATE", c, null, null, 900, Priority.HIGH, TaskStatus.REQUESTED);
        MaintenanceTask t2 = task("T2-DATE", c, null, null, 900, Priority.HIGH, TaskStatus.REQUESTED);
        block("BLK-D1", t1, c, D, D, BlockRequestStatus.REQUESTED);
        block("BLK-D2", t2, c, D.plusDays(10), D.plusDays(10), BlockRequestStatus.REQUESTED);

        // Range covers only the second block; results must only mention it.
        List<ConflictResponse> conflicts = service.detectAll(null, D.plusDays(9), D.plusDays(11));

        assertThat(conflicts)
                .allMatch(x -> x.blockCode().equals("BLK-D2"));
    }

    @Test
    void cancelledAndRejectedBlocksAreIgnored() {
        Corridor c = corridor("COR-IGN");
        MaintenanceTask t1 = task("T1-IGN", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        MaintenanceTask t2 = task("T2-IGN", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        MaintenanceTask t3 = task("T3-IGN", c, null, null, 60, Priority.LOW, TaskStatus.REQUESTED);
        block("BLK-ACTIVE", t1, c, D, D.plusDays(2), BlockRequestStatus.REQUESTED);
        block("BLK-CANCELLED", t2, c, D, D.plusDays(2), BlockRequestStatus.CANCELLED);
        block("BLK-REJECTED", t3, c, D, D.plusDays(2), BlockRequestStatus.REJECTED);

        List<ConflictResponse> conflicts = service.detectAll(null, null, null);

        assertThat(conflicts)
                .noneMatch(x -> x.blockCode().equals("BLK-CANCELLED"))
                .noneMatch(x -> x.blockCode().equals("BLK-REJECTED"));
    }

    @Test
    void detectAllRejectsReversedDateRange() {
        assertThatThrownBy(() -> service.detectAll(null, D.plusDays(5), D))
                .isInstanceOf(com.ps27.railway.exception.BadRequestException.class);
    }

    @Test
    void detectAllRejectsMissingCounterpartFilter() {
        assertThatThrownBy(() -> service.detectAll(null, D, null))
                .isInstanceOf(com.ps27.railway.exception.BadRequestException.class);
    }

    @Test
    void detectAllUnknownCorridorThrowsNotFound() {
        assertThatThrownBy(() -> service.detectAll(999999L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resultsAreDeterministicallyOrdered() {
        Corridor c = corridor("COR-ORD");
        Asset a = asset(c, "AST-ORD");
        Department dept = department("DEPT-ORD");
        resourceRepository.save(new MaintenanceResource("RES-ORD", "Crew", ResourceType.CREW,
                dept, 1, true));
        Train train = trainRepository.save(new Train("TRN-ORD", "Ord Train", TrainType.PASSENGER, true));
        trainScheduleRepository.save(new TrainSchedule(train, c, D, null,
                LocalTime.of(6, 0), LocalTime.of(8, 0), Direction.UP));

        // Task with a huge duration producing many kinds of conflicts.
        MaintenanceTask t1 = new MaintenanceTask("T1-ORD", "Long task", "desc", TaskType.INSPECTION,
                Priority.CRITICAL, 900, c, a, dept, null, null,
                null, null, TaskStatus.REQUESTED, null);
        taskRepository.save(t1);
        // t2 shares asset + department with t1 so spatial/resource conflicts also appear.
        MaintenanceTask t2 = task("T2-ORD", c, dept, a, 60, Priority.LOW, TaskStatus.REQUESTED);
        // t3 starts on D+2 -> 0 free days after BLK-O2 ends on D+1 -> insufficient gap.
        MaintenanceTask t3 = task("T3-ORD", c, dept, a, 60, Priority.LOW, TaskStatus.REQUESTED);
        block("BLK-O1", t1, c, D, D, BlockRequestStatus.REQUESTED);
        block("BLK-O2", t2, c, D, D.plusDays(1), BlockRequestStatus.REQUESTED);
        block("BLK-O3", t3, c, D.plusDays(2), D.plusDays(2), BlockRequestStatus.REQUESTED);

        List<ConflictResponse> first = service.detectAll(null, null, null);
        List<ConflictResponse> second = service.detectAll(null, null, null);

        assertThat(first).hasSameSizeAs(second);
        assertThat(first).containsExactlyElementsOf(second);
        assertThat(first)
                .anyMatch(x -> x.type() == ConflictType.TRAIN_OVERLAP)
                .anyMatch(x -> x.type() == ConflictType.BLOCK_OVERLAP)
                .anyMatch(x -> x.type() == ConflictType.SPATIAL_OVERLAP)
                .anyMatch(x -> x.type() == ConflictType.INSUFFICIENT_GAP)
                .anyMatch(x -> x.type() == ConflictType.RESOURCE_CONFLICT)
                .anyMatch(x -> x.type() == ConflictType.DURATION_FIT);
    }
}

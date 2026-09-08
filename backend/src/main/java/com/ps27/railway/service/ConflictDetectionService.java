package com.ps27.railway.service;

import com.ps27.railway.dto.BlockRequestRequest;
import com.ps27.railway.dto.ConflictCheckResponse;
import com.ps27.railway.dto.ConflictResponse;
import com.ps27.railway.entity.Asset;
import com.ps27.railway.entity.BlockRequest;
import com.ps27.railway.entity.Constraint;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.MaintenanceResource;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.entity.TrainSchedule;
import com.ps27.railway.enums.BlockRequestStatus;
import com.ps27.railway.enums.ConflictSeverity;
import com.ps27.railway.enums.ConflictType;
import com.ps27.railway.enums.ConstraintType;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TrainType;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.BlockRequestRepository;
import com.ps27.railway.repository.ConstraintRepository;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.MaintenanceResourceRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import com.ps27.railway.repository.TrainScheduleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic, explainable conflict detection (Part 4 / plan part2).
 *
 * <p>Active blocks are those with a status in {DRAFT, REQUESTED, APPROVED, SCHEDULED}.
 * REJECTED and CANCELLED blocks are ignored. All checks are pure functions over the
 * existing Part 1 data - no AI, no randomness, no persistence of results.
 */
@Service
public class ConflictDetectionService {

    /** Block statuses that participate in conflict detection. */
    private static final Set<BlockRequestStatus> ACTIVE_STATUSES = EnumSet.of(
            BlockRequestStatus.DRAFT,
            BlockRequestStatus.REQUESTED,
            BlockRequestStatus.APPROVED,
            BlockRequestStatus.SCHEDULED);

    /** Corridor statuses that are safe for a new block. */
    private static final Set<CorridorStatus> OPERATIONAL_STATUSES = EnumSet.of(
            CorridorStatus.OPERATIONAL);

    private static final Comparator<ConflictResponse> RESULT_ORDER =
            Comparator.comparing(ConflictResponse::blockCode, Comparator.nullsLast(String::compareTo))
                    .thenComparing(c -> c.date() == null ? LocalDate.MAX : c.date())
                    .thenComparing(c -> c.type().name())
                    .thenComparing(c -> c.details() == null ? "" : c.details());

    private final BlockRequestRepository blockRequestRepository;
    private final TrainScheduleRepository trainScheduleRepository;
    private final MaintenanceResourceRepository resourceRepository;
    private final ConstraintRepository constraintRepository;
    private final CorridorRepository corridorRepository;
    private final MaintenanceTaskRepository taskRepository;

    private final int minGapDays;
    private final int maxDailyWorkMinutes;

    public ConflictDetectionService(BlockRequestRepository blockRequestRepository,
                                    TrainScheduleRepository trainScheduleRepository,
                                    MaintenanceResourceRepository resourceRepository,
                                    ConstraintRepository constraintRepository,
                                    CorridorRepository corridorRepository,
                                    MaintenanceTaskRepository taskRepository,
                                    @Value("${conflict.min-gap-days:1}") int minGapDays,
                                    @Value("${conflict.max-daily-work-minutes:720}") int maxDailyWorkMinutes) {
        this.blockRequestRepository = blockRequestRepository;
        this.trainScheduleRepository = trainScheduleRepository;
        this.resourceRepository = resourceRepository;
        this.constraintRepository = constraintRepository;
        this.corridorRepository = corridorRepository;
        this.taskRepository = taskRepository;
        this.minGapDays = minGapDays;
        this.maxDailyWorkMinutes = maxDailyWorkMinutes;
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Detects conflicts for all active blocks. Optional {@code corridorId} and
     * {@code from}/{@code to} (both required together) narrow the evaluated set.
     */
    @Transactional(readOnly = true)
    public List<ConflictResponse> detectAll(Long corridorId, LocalDate from, LocalDate to) {
        validateFilter(corridorId, from, to);
        List<BlockRequest> scope = activeBlocks(corridorId, from, to);
        List<ConflictResponse> result = new ArrayList<>();
        for (BlockRequest subject : scope) {
            List<BlockRequest> others = othersIn(scope, subject);
            result.addAll(evaluateSubject(subject, others));
        }
        return sort(result);
    }

    /** Detects conflicts for a single persisted block. Unknown id yields 404. */
    @Transactional(readOnly = true)
    public List<ConflictResponse> detectForBlock(Long blockId) {
        BlockRequest subject = blockRequestRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Block request not found: " + blockId));
        List<BlockRequest> others = activeBlocks(null, null, null).stream()
                .filter(b -> !b.getId().equals(subject.getId()))
                .toList();
        return sort(evaluateSubject(subject, others));
    }

    /**
     * What-if check: evaluates a proposed block request against existing active
     * blocks, train schedules and constraints WITHOUT persisting anything.
     */
    @Transactional(readOnly = true)
    public ConflictCheckResponse checkProposed(BlockRequestRequest request) {
        validateProposed(request);
        MaintenanceTask task = taskRepository.findById(request.maintenanceTaskId())
                .orElseThrow(() -> new BadRequestException("Maintenance task not found: " + request.maintenanceTaskId()));
        Corridor corridor = corridorRepository.findById(request.corridorId())
                .orElseThrow(() -> new BadRequestException("Corridor not found: " + request.corridorId()));

        // Transient (non-persisted) candidate block used only for evaluation.
        BlockRequest candidate = new BlockRequest(request.blockCode(), task, corridor,
                request.requestedStart(), request.requestedEnd(),
                request.status() != null ? request.status() : BlockRequestStatus.REQUESTED,
                request.notes());

        List<BlockRequest> existing = activeBlocks(null, null, null);
        return new ConflictCheckResponse(request.blockCode(), sort(evaluateSubject(candidate, existing)));
    }

    /**
     * Returns true when the task already has at least one active block
     * (DRAFT/REQUESTED/APPROVED/SCHEDULED). Used by the optimizer to skip tasks
     * that are already planned.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveBlockForTask(Long taskId) {
        return activeBlocks(null, null, null).stream()
                .anyMatch(b -> b.getMaintenanceTask().getId().equals(taskId));
    }

    /**
     * Counts active blocks (DRAFT/REQUESTED/APPROVED/SCHEDULED) covering the
     * given date, regardless of corridor. Used by the optimizer to enforce the
     * configured maximum number of concurrent blocks per day.
     */
    @Transactional(readOnly = true)
    public long countActiveBlocksOnDate(LocalDate date) {
        return activeBlocks(null, null, null).stream()
                .filter(b -> overlaps(b, date, date))
                .count();
    }

    // ------------------------------------------------------------------
    // Subject evaluation - produces conflicts from the perspective of one block
    // ------------------------------------------------------------------

    private List<ConflictResponse> evaluateSubject(BlockRequest subject, List<BlockRequest> others) {
        List<ConflictResponse> found = new ArrayList<>();

        detectTrainOverlaps(subject, found);
        detectBlockOverlaps(subject, others, found);
        detectSpatialOverlaps(subject, others, found);
        detectInsufficientGap(subject, others, found);
        detectResourceConflicts(subject, others, found);
        detectConstraintViolations(subject, found);
        detectDurationFit(subject, found);
        return found;
    }

    // --- TRAIN_OVERLAP -------------------------------------------------

    private void detectTrainOverlaps(BlockRequest block, List<ConflictResponse> out) {
        List<TrainSchedule> schedules = trainScheduleRepository.findAll().stream()
                .filter(s -> s.getCorridor().getId().equals(block.getCorridor().getId()))
                .sorted(Comparator.comparing(TrainSchedule::getId))
                .toList();

        LocalDate d = block.getRequestedStart();
        while (!d.isAfter(block.getRequestedEnd())) {
            for (TrainSchedule s : schedules) {
                if (matchesDate(s, d)) {
                    String trainCode = s.getTrain().getCode();
                    String trainName = s.getTrain().getName();
                    TrainType type = s.getTrain().getTrainType();
                    ConflictSeverity severity = (type == TrainType.PASSENGER || type == TrainType.SUBURBAN)
                            ? ConflictSeverity.HIGH : ConflictSeverity.MEDIUM;
                    String details = "train " + trainCode + " on " + d;
                    String explanation = blockDescription(block) + " overlaps "
                            + trainCode + " " + trainName + " (" + type + ") on corridor "
                            + block.getCorridor().getCode() + " on " + d;
                    out.add(conflict(ConflictType.TRAIN_OVERLAP, severity, block, d, details, explanation));
                }
            }
            d = d.plusDays(1);
        }
    }

    private boolean matchesDate(TrainSchedule s, LocalDate date) {
        if (s.getScheduleDate() != null) {
            return s.getScheduleDate().equals(date);
        }
        DayOfWeek dow = s.getDayOfWeek();
        return dow != null && date.getDayOfWeek() == dow;
    }

    // --- BLOCK_OVERLAP -------------------------------------------------

    private void detectBlockOverlaps(BlockRequest subject, List<BlockRequest> others,
                                     List<ConflictResponse> out) {
        for (BlockRequest other : others) {
            if (!subject.getCorridor().getId().equals(other.getCorridor().getId())) {
                continue;
            }
            if (overlaps(subject, other)) {
                boolean critical = isCritical(subject) || isCritical(other);
                ConflictSeverity severity = critical ? ConflictSeverity.HIGH : ConflictSeverity.MEDIUM;
                LocalDate overlapStart = max(subject.getRequestedStart(), other.getRequestedStart());
                String details = "overlaps " + other.getBlockCode() + " on "
                        + subject.getCorridor().getCode();
                String explanation = blockDescription(subject) + " overlaps block "
                        + other.getBlockCode() + " (" + taskTitle(other) + ") on corridor "
                        + subject.getCorridor().getCode() + " from " + overlapStart;
                out.add(conflict(ConflictType.BLOCK_OVERLAP, severity, subject,
                        overlapStart, details, explanation));
            }
        }
    }

    // --- SPATIAL_OVERLAP ----------------------------------------------

    private void detectSpatialOverlaps(BlockRequest subject, List<BlockRequest> others,
                                       List<ConflictResponse> out) {
        Asset subjectAsset = subject.getMaintenanceTask().getAsset();
        if (subjectAsset == null) {
            return;
        }
        for (BlockRequest other : others) {
            if (!overlaps(subject, other)) {
                continue;
            }
            Asset otherAsset = other.getMaintenanceTask().getAsset();
            if (otherAsset == null || !subjectAsset.getId().equals(otherAsset.getId())) {
                continue;
            }
            boolean critical = isCritical(subject) || isCritical(other);
            ConflictSeverity severity = critical ? ConflictSeverity.HIGH : ConflictSeverity.MEDIUM;
            LocalDate overlapStart = max(subject.getRequestedStart(), other.getRequestedStart());
            String details = "same asset " + subjectAsset.getCode() + " as " + other.getBlockCode();
            String explanation = blockDescription(subject) + " and block "
                    + other.getBlockCode() + " (" + taskTitle(other) + ") both target asset "
                    + subjectAsset.getCode() + " (" + subjectAsset.getName() + ") on "
                    + overlapStart;
            out.add(conflict(ConflictType.SPATIAL_OVERLAP, severity, subject,
                    overlapStart, details, explanation));
        }
    }

    // --- INSUFFICIENT_GAP ----------------------------------------------

    private void detectInsufficientGap(BlockRequest subject, List<BlockRequest> others,
                                       List<ConflictResponse> out) {
        List<BlockRequest> corridorBlocks = new ArrayList<>(others);
        corridorBlocks.add(subject);
        corridorBlocks = corridorBlocks.stream()
                .filter(b -> b.getCorridor().getId().equals(subject.getCorridor().getId()))
                .sorted(blockOrder())
                .toList();

        int index = indexOf(corridorBlocks, subject);
        if (index < 0) {
            return;
        }
        // Gap to the previous non-overlapping block (subject starts too soon after it).
        for (int i = index - 1; i >= 0; i--) {
            BlockRequest prev = corridorBlocks.get(i);
            if (overlaps(prev, subject)) {
                continue; // already reported as BLOCK_OVERLAP
            }
            long freeGapDays = ChronoUnit.DAYS.between(prev.getRequestedEnd(), subject.getRequestedStart()) - 1;
            if (freeGapDays < minGapDays) {
                String details = "only " + freeGapDays + " free day(s) after " + prev.getBlockCode();
                String explanation = "Block " + subject.getBlockCode() + " (" + taskTitle(subject)
                        + ") starts " + subject.getRequestedStart() + ", only " + freeGapDays
                        + " free day(s) after block " + prev.getBlockCode() + " ends "
                        + prev.getRequestedEnd() + " on corridor " + subject.getCorridor().getCode()
                        + "; minimum gap is " + minGapDays + " day(s)";
                out.add(conflict(ConflictType.INSUFFICIENT_GAP, ConflictSeverity.LOW, subject,
                        subject.getRequestedStart(), details, explanation));
            }
            break;
        }
        // Gap to the next non-overlapping block (too little free time before it).
        for (int i = index + 1; i < corridorBlocks.size(); i++) {
            BlockRequest next = corridorBlocks.get(i);
            if (overlaps(subject, next)) {
                continue;
            }
            long freeGapDays = ChronoUnit.DAYS.between(subject.getRequestedEnd(), next.getRequestedStart()) - 1;
            if (freeGapDays < minGapDays) {
                String details = "only " + freeGapDays + " free day(s) before " + next.getBlockCode();
                String explanation = "Block " + subject.getBlockCode() + " (" + taskTitle(subject)
                        + ") ends " + subject.getRequestedEnd() + ", only " + freeGapDays
                        + " free day(s) before block " + next.getBlockCode() + " starts "
                        + next.getRequestedStart() + " on corridor " + subject.getCorridor().getCode()
                        + "; minimum gap is " + minGapDays + " day(s)";
                out.add(conflict(ConflictType.INSUFFICIENT_GAP, ConflictSeverity.LOW, subject,
                        next.getRequestedStart(), details, explanation));
            }
            break;
        }
    }

    // --- RESOURCE_CONFLICT ---------------------------------------------

    private void detectResourceConflicts(BlockRequest subject, List<BlockRequest> others,
                                         List<ConflictResponse> out) {
        MaintenanceTask task = subject.getMaintenanceTask();
        if (task.getDepartment() == null) {
            return;
        }
        Long deptId = task.getDepartment().getId();
        int capacity = capacityFor(task.getDepartment().getId());

        List<BlockRequest> all = new ArrayList<>(others);
        all.add(subject);

        // Count blocks of the same department covering each date.
        Map<LocalDate, List<BlockRequest>> byDate = new HashMap<>();
        for (BlockRequest b : all) {
            MaintenanceTask bt = b.getMaintenanceTask();
            if (bt.getDepartment() == null || !bt.getDepartment().getId().equals(deptId)) {
                continue;
            }
            LocalDate d = b.getRequestedStart();
            while (!d.isAfter(b.getRequestedEnd())) {
                byDate.computeIfAbsent(d, k -> new ArrayList<>()).add(b);
                d = d.plusDays(1);
            }
        }

        LocalDate d = subject.getRequestedStart();
        while (!d.isAfter(subject.getRequestedEnd())) {
            List<BlockRequest> thatDay = byDate.getOrDefault(d, List.of());
            int needed = thatDay.size();
            if (needed > capacity) {
                String details = "department " + task.getDepartment().getCode()
                        + " needs " + needed + " team(s) on " + d + ", capacity " + capacity;
                String explanation = "On " + d + ", " + needed + " active block(s) of department "
                        + task.getDepartment().getCode() + " exceed its combined resource capacity of "
                        + capacity + " (block " + blockDescription(subject) + " among them)";
                out.add(conflict(ConflictType.RESOURCE_CONFLICT, ConflictSeverity.MEDIUM,
                        subject, d, details, explanation));
            }
            d = d.plusDays(1);
        }
    }

    private int capacityFor(Long departmentId) {
        int total = 0;
        for (MaintenanceResource r : resourceRepository.findAll()) {
            if (r.isActive() && r.getDepartment() != null
                    && r.getDepartment().getId().equals(departmentId)) {
                total += r.getCapacityPerShift();
            }
        }
        return total;
    }

    // --- CONSTRAINT_VIOLATION ------------------------------------------

    private void detectConstraintViolations(BlockRequest block, List<ConflictResponse> out) {
        MaintenanceTask task = block.getMaintenanceTask();
        LocalDate start = block.getRequestedStart();
        LocalDate end = block.getRequestedEnd();

        // Allowed window [earliestStart, latestEnd].
        if (task.getEarliestStart() != null && start.isBefore(task.getEarliestStart())) {
            String explanation = blockDescription(block) + " starts " + start
                    + " before the earliest allowed start " + task.getEarliestStart()
                    + " for task " + task.getTaskCode();
            out.add(conflict(ConflictType.CONSTRAINT_VIOLATION, ConflictSeverity.HIGH, block,
                    start, "before earliest start " + task.getEarliestStart(), explanation));
        }
        if (task.getLatestEnd() != null && end.isAfter(task.getLatestEnd())) {
            String explanation = blockDescription(block) + " ends " + end
                    + " after the latest allowed end " + task.getLatestEnd()
                    + " for task " + task.getTaskCode();
            out.add(conflict(ConflictType.CONSTRAINT_VIOLATION, ConflictSeverity.HIGH, block,
                    end, "after latest end " + task.getLatestEnd(), explanation));
        }

        // Corridor must be operational.
        if (!OPERATIONAL_STATUSES.contains(block.getCorridor().getStatus())) {
            String explanation = blockDescription(block) + " is on corridor "
                    + block.getCorridor().getCode() + " whose status is "
                    + block.getCorridor().getStatus() + " (not OPERATIONAL)";
            out.add(conflict(ConflictType.CONSTRAINT_VIOLATION, ConflictSeverity.HIGH, block,
                    start, "corridor not OPERATIONAL", explanation));
        }

        // Unmet dependencies (a dependency task not COMPLETED).
        task.getDependencies().stream()
                .sorted(Comparator.comparing(MaintenanceTask::getId))
                .filter(dep -> dep.getStatus() != TaskStatus.COMPLETED)
                .forEach(dep -> {
                    String explanation = blockDescription(block) + " requires task "
                            + dep.getTaskCode() + " (" + taskTitleOf(dep) + ") first, but its status is "
                            + dep.getStatus();
                    out.add(conflict(ConflictType.CONSTRAINT_VIOLATION, ConflictSeverity.MEDIUM,
                            block, start, "dependency " + dep.getTaskCode() + " not COMPLETED", explanation));
                });

        // Active safety / operational rules attached to the task (or global).
        constraintRepository.findByActiveTrue().stream()
                .filter(c -> c.getTask() == null || c.getTask().getId().equals(task.getId()))
                .filter(c -> c.getConstraintType() == ConstraintType.SAFETY_RULE
                        || c.getConstraintType() == ConstraintType.OPERATIONAL_RULE)
                .sorted(Comparator.comparing(Constraint::getId))
                .forEach(c -> {
                    String explanation = blockDescription(block) + " violates active "
                            + c.getConstraintType() + " constraint \"" + c.getName()
                            + "\" on task " + task.getTaskCode();
                    out.add(conflict(ConflictType.CONSTRAINT_VIOLATION, ConflictSeverity.HIGH,
                            block, start, "constraint " + c.getName(), explanation));
                });
    }

    // --- DURATION_FIT ---------------------------------------------------

    private void detectDurationFit(BlockRequest block, List<ConflictResponse> out) {
        if (!block.getRequestedStart().equals(block.getRequestedEnd())) {
            return; // only single-day blocks are checked
        }
        int duration = block.getMaintenanceTask().getDurationMinutes();
        if (duration > maxDailyWorkMinutes) {
            String details = "duration " + duration + " min exceeds daily limit " + maxDailyWorkMinutes + " min";
            String explanation = blockDescription(block) + " requires " + duration
                    + " minutes of work on a single day, exceeding the configured daily limit of "
                    + maxDailyWorkMinutes + " minutes";
            out.add(conflict(ConflictType.DURATION_FIT, ConflictSeverity.HIGH, block,
                    block.getRequestedStart(), details, explanation));
        }
    }

    // ------------------------------------------------------------------
    // Loading / filtering helpers
    // ------------------------------------------------------------------

    private List<BlockRequest> activeBlocks(Long corridorId, LocalDate from, LocalDate to) {
        List<BlockRequest> all = new ArrayList<>(blockRequestRepository.findAll());
        return all.stream()
                .filter(b -> ACTIVE_STATUSES.contains(b.getStatus()))
                .filter(b -> corridorId == null || b.getCorridor().getId().equals(corridorId))
                .filter(b -> from == null || to == null || overlaps(b, from, to))
                .toList();
    }

    private List<BlockRequest> othersIn(List<BlockRequest> scope, BlockRequest subject) {
        return scope.stream()
                .filter(b -> !sameBlock(b, subject))
                .toList();
    }

    private void validateFilter(Long corridorId, LocalDate from, LocalDate to) {
        if (corridorId != null && !corridorRepository.existsById(corridorId)) {
            throw new ResourceNotFoundException("Corridor not found: " + corridorId);
        }
        if ((from == null) != (to == null)) {
            throw new BadRequestException("Both 'from' and 'to' must be provided together");
        }
        if (from != null && to.isBefore(from)) {
            throw new BadRequestException("'to' must be after or equal to 'from'");
        }
    }

    private void validateProposed(BlockRequestRequest request) {
        if (request.requestedEnd().isBefore(request.requestedStart())) {
            throw new BadRequestException("requestedEnd must be after or equal to requestedStart");
        }
    }

    private static boolean overlaps(BlockRequest a, BlockRequest b) {
        return !a.getRequestedEnd().isBefore(b.getRequestedStart())
                && !b.getRequestedEnd().isBefore(a.getRequestedStart());
    }

    private static boolean overlaps(BlockRequest b, LocalDate from, LocalDate to) {
        return !b.getRequestedEnd().isBefore(from) && !to.isBefore(b.getRequestedStart());
    }

    private static boolean isCritical(BlockRequest b) {
        return b.getMaintenanceTask().getPriority() == Priority.CRITICAL;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static boolean sameBlock(BlockRequest a, BlockRequest b) {
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a == b;
    }

    private static int indexOf(List<BlockRequest> sorted, BlockRequest target) {
        for (int i = 0; i < sorted.size(); i++) {
            if (sameBlock(sorted.get(i), target)) {
                return i;
            }
        }
        return -1;
    }

    private static Comparator<BlockRequest> blockOrder() {
        return Comparator
                .comparing(BlockRequest::getRequestedStart)
                .thenComparing(BlockRequest::getRequestedEnd)
                .thenComparing(BlockRequest::getBlockCode);
    }

    private String blockDescription(BlockRequest b) {
        return "Block " + b.getBlockCode() + " (" + taskTitle(b) + ")";
    }

    private String taskTitle(BlockRequest b) {
        return taskTitleOf(b.getMaintenanceTask());
    }

    private static String taskTitleOf(MaintenanceTask t) {
        return t.getTitle() == null ? t.getTaskCode() : t.getTitle();
    }

    private ConflictResponse conflict(ConflictType type, ConflictSeverity severity, BlockRequest block,
                                      LocalDate date, String details, String explanation) {
        MaintenanceTask task = block.getMaintenanceTask();
        Corridor corridor = block.getCorridor();
        return ConflictResponse.of(type, severity,
                block.getId(), block.getBlockCode(),
                task.getId(), task.getTaskCode(), task.getTitle(),
                corridor.getId(), corridor.getCode(),
                date, details, explanation);
    }

    private static List<ConflictResponse> sort(List<ConflictResponse> conflicts) {
        return conflicts.stream().sorted(RESULT_ORDER).toList();
    }
}

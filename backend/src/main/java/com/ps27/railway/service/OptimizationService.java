package com.ps27.railway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ps27.railway.dto.BlockRequestRequest;
import com.ps27.railway.dto.ConflictResponse;
import com.ps27.railway.dto.OptimizationResultResponse;
import com.ps27.railway.dto.OptimizationRunDetailResponse;
import com.ps27.railway.dto.OptimizationRunRequest;
import com.ps27.railway.dto.OptimizationRunRequest.BlackoutWindow;
import com.ps27.railway.dto.OptimizationRunResponse;
import com.ps27.railway.entity.BlockRequest;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.entity.OptimizationResult;
import com.ps27.railway.entity.OptimizationRun;
import com.ps27.railway.enums.BlockRequestStatus;
import com.ps27.railway.enums.ConflictSeverity;
import com.ps27.railway.enums.ConflictType;
import com.ps27.railway.enums.OptimizationDecision;
import com.ps27.railway.enums.OptimizationStatus;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.exception.ResourceNotFoundException;
import com.ps27.railway.repository.BlockRequestRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import com.ps27.railway.repository.OptimizationResultRepository;
import com.ps27.railway.repository.OptimizationRunRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic, explainable maintenance block optimizer (Part 5 / plan part3).
 *
 * <p>Algorithm {@code deterministic-greedy-v1}: tasks are ranked by priority
 * weight (CRITICAL 100 > HIGH 60 > MEDIUM 30 > LOW 10), then duration,
 * then code. For each task, candidates are enumerated as every start date in the
 * clipped allowed window with block length = max(1, ceil(duration /
 * maxDailyWorkMinutes)). Feasibility reuses {@link ConflictDetectionService}
 * (what-if checks): hard rejects and soft flags. Scoring = priorityWeight +
 * 5*daysEarly - 15*softConflicts; tie-break earliest start, then candidate code.
 * Selected candidates are persisted as {@link BlockRequest}s so later candidates
 * see them through the same what-if checks. No AI/ML, no randomness — identical
 * input produces identical output.
 */
@Service
public class OptimizationService {

    private static final int PRIORITY_CRITICAL = 100;
    private static final int PRIORITY_HIGH = 60;
    private static final int PRIORITY_MEDIUM = 30;
    private static final int PRIORITY_LOW = 10;
    private static final int DAYS_EARLY_WEIGHT = 5;
    private static final int SOFT_CONFLICT_PENALTY = 15;

    /** Conflict types that are hard rejects for a candidate. */
    private static final Set<ConflictType> HARD_REJECT_TYPES = EnumSet.of(
            ConflictType.TRAIN_OVERLAP,
            ConflictType.BLOCK_OVERLAP,
            ConflictType.SPATIAL_OVERLAP,
            ConflictType.RESOURCE_CONFLICT,
            ConflictType.DURATION_FIT);

    private final OptimizationRunRepository runRepository;
    private final OptimizationResultRepository resultRepository;
    private final MaintenanceTaskRepository taskRepository;
    private final BlockRequestRepository blockRequestRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final ObjectMapper objectMapper;

    private final String algorithm;
    private final int defaultMaxConcurrentBlocks;
    private final BlockRequestStatus persistBlockStatus;
    private final int maxDailyWorkMinutes;

    public OptimizationService(OptimizationRunRepository runRepository,
                               OptimizationResultRepository resultRepository,
                               MaintenanceTaskRepository taskRepository,
                               BlockRequestRepository blockRequestRepository,
                               ConflictDetectionService conflictDetectionService,
                               ObjectMapper objectMapper,
                               @Value("${optimization.algorithm:deterministic-greedy-v1}") String algorithm,
                               @Value("${optimization.max-concurrent-blocks:5}") int defaultMaxConcurrentBlocks,
                               @Value("${optimization.persist-block-status:SCHEDULED}") BlockRequestStatus persistBlockStatus,
                               @Value("${conflict.max-daily-work-minutes:720}") int maxDailyWorkMinutes) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.taskRepository = taskRepository;
        this.blockRequestRepository = blockRequestRepository;
        this.conflictDetectionService = conflictDetectionService;
        this.objectMapper = objectMapper;
        this.algorithm = algorithm;
        this.defaultMaxConcurrentBlocks = defaultMaxConcurrentBlocks;
        this.persistBlockStatus = persistBlockStatus;
        this.maxDailyWorkMinutes = maxDailyWorkMinutes;
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Runs the deterministic optimizer for the given window and returns the full
     * run detail (summary + per-task results). Selected candidates are persisted
     * as {@link BlockRequest}s by default.
     */
    @Transactional
    public OptimizationRunDetailResponse run(OptimizationRunRequest request) {
        validateRequest(request);
        int maxConcurrentBlocks = request.maxConcurrentBlocks() != null
                ? request.maxConcurrentBlocks() : defaultMaxConcurrentBlocks;
        boolean persistBlocks = request.persistBlocks() == null || request.persistBlocks();

        List<MaintenanceTask> tasks = loadEligibleTasks(request);

        OptimizationRun run = new OptimizationRun(algorithm, OptimizationStatus.COMPLETED,
                request.windowStart(), request.windowEnd(), 0, 0, 0, 0, 0,
                "", "");
        runRepository.save(run);

        List<OptimizationResult> results = new ArrayList<>();
        List<String> rejectedCandidates = new ArrayList<>();
        int totalConflicts = 0;
        int totalCandidates = 0;
        int objective = 0;

        for (MaintenanceTask task : tasks) {
            if (conflictDetectionService.hasActiveBlockForTask(task.getId())) {
                results.add(rejected(task, "Task already has an active block; skipped", null, List.of()));
                continue;
            }
            List<LocalDate> starts = candidateStarts(task, request);
            totalCandidates += starts.size();

            if (starts.isEmpty()) {
                results.add(rejected(task, "No feasible start date inside the allowed window",
                        null, List.of()));
                continue;
            }

            // Best candidate per task (score desc, earliest start, candidate code asc).
            Candidate best = null;
            List<Candidate> evaluated = new ArrayList<>();
            for (LocalDate start : starts) {
                LocalDate end = start.plusDays(blockLengthDays(task) - 1);
                Candidate candidate = evaluateCandidate(task, start, end, request,
                        maxConcurrentBlocks);
                totalConflicts += candidate.conflicts().size();
                evaluated.add(candidate);
                if (best == null || candidate.betterThan(best)) {
                    best = candidate;
                }
            }
            rejectedCandidates.addAll(reasonStrings(evaluated));

            if (best.feasible()) {
                BlockRequest block = persistBlocks ? persistCandidate(task, best) : null;
                String blockCode = block != null ? block.getBlockCode() : best.blockCode();
                Long blockId = block != null ? block.getId() : null;
                results.add(scheduled(task, best, blockId, blockCode));
                objective += best.score();
            } else {
                results.add(rejected(task, "No feasible candidate; best candidate rejected because "
                        + best.reason(), best.score(), best.conflicts()));
            }
        }

        for (OptimizationResult result : results) {
            run.addResult(result);
        }
        run.setTasksScheduled((int) results.stream()
                .filter(r -> r.getDecision() == OptimizationDecision.SCHEDULED).count());
        run.setTasksRejected((int) results.stream()
                .filter(r -> r.getDecision() == OptimizationDecision.REJECTED).count());
        run.setConflictsDetected(totalConflicts);
        run.setCandidatesEvaluated(totalCandidates);
        run.setObjectiveScore(objective);
        run.setRejectedCandidatesJson(toJson(rejectedCandidates));
        run.setSummaryReason(summaryReason(run, rejectedCandidates.size()));
        OptimizationRun saved = runRepository.save(run);

        return toDetail(saved);
    }

    @Transactional(readOnly = true)
    public OptimizationRunResponse getRun(Long id) {
        return toSummary(getRunEntity(id));
    }

    @Transactional(readOnly = true)
    public List<OptimizationResultResponse> getResults(Long runId) {
        OptimizationRun run = getRunEntity(runId);
        return resultRepository.findByRunIdOrderById(run.getId()).stream()
                .map(this::toResultResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Deterministic optimization steps
    // ------------------------------------------------------------------

    private List<MaintenanceTask> loadEligibleTasks(OptimizationRunRequest request) {
        return taskRepository.findAll().stream()
                .filter(t -> request.taskIds() == null || request.taskIds().contains(t.getId()))
                .filter(t -> eligibleStatus(t.getStatus()))
                .filter(t -> allowedWindowOverlaps(t, request))
                .sorted(taskRank())
                .toList();
    }

    private boolean allowedWindowOverlaps(MaintenanceTask t, OptimizationRunRequest request) {
        LocalDate from = t.getEarliestStart() != null ? t.getEarliestStart()
                : (t.getRequestedStart() != null ? t.getRequestedStart() : request.windowStart());
        LocalDate to = t.getLatestEnd() != null ? t.getLatestEnd()
                : (t.getRequestedEnd() != null ? t.getRequestedEnd() : request.windowEnd());
        return !from.isAfter(request.windowEnd()) && !request.windowStart().isAfter(to);
    }

    private static boolean eligibleStatus(TaskStatus status) {
        return status == TaskStatus.DRAFT
                || status == TaskStatus.REQUESTED
                || status == TaskStatus.APPROVED;
    }

    private List<LocalDate> candidateStarts(MaintenanceTask task, OptimizationRunRequest request) {
        LocalDate from = max(task.getEarliestStart() != null ? task.getEarliestStart()
                : (task.getRequestedStart() != null ? task.getRequestedStart() : request.windowStart()),
                request.windowStart());
        LocalDate to = min(task.getLatestEnd() != null ? task.getLatestEnd()
                : (task.getRequestedEnd() != null ? task.getRequestedEnd() : request.windowEnd()),
                request.windowEnd());
        int length = blockLengthDays(task);
        List<LocalDate> starts = new ArrayList<>();
        LocalDate d = from;
        while (!d.isAfter(to)) {
            if (!d.plusDays(length - 1).isAfter(to)) {
                starts.add(d);
            }
            d = d.plusDays(1);
        }
        return starts;
    }

    private Candidate evaluateCandidate(MaintenanceTask task, LocalDate start, LocalDate end,
                                        OptimizationRunRequest request, int maxConcurrentBlocks) {
        List<ConflictResponse> conflicts = conflictDetectionService.checkProposed(
                        new BlockRequestRequest(candidateCode(task, start), task.getId(),
                                task.getCorridor().getId(), start, end,
                                BlockRequestStatus.REQUESTED, "optimizer candidate"))
                .conflicts();

        String reason = null;
        List<ConflictResponse> soft = new ArrayList<>();
        for (ConflictResponse c : conflicts) {
            if (isHardReject(c)) {
                reason = "conflict " + c.type() + " (severity " + c.severity()
                        + "): " + c.details();
                break;
            }
            soft.add(c);
        }
        if (reason == null && overlapsAnyBlackout(start, end, request)) {
            reason = "overlaps a configured blackout window";
        }
        if (reason == null && exceedsConcurrentBlocks(start, end, maxConcurrentBlocks)) {
            reason = "exceeds maximum concurrent blocks (" + maxConcurrentBlocks + ") on a day";
        }

        int score = score(task, start, end, request, soft.size());
        return new Candidate(candidateCode(task, start), start, end, score, reason, conflicts);
    }

    private boolean exceedsConcurrentBlocks(LocalDate start, LocalDate end, int maxConcurrentBlocks) {
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (conflictDetectionService.countActiveBlocksOnDate(d) >= maxConcurrentBlocks) {
                return true;
            }
            d = d.plusDays(1);
        }
        return false;
    }

    private static boolean overlapsAnyBlackout(LocalDate start, LocalDate end,
                                               OptimizationRunRequest request) {
        if (request.blackoutWindows() == null) {
            return false;
        }
        for (BlackoutWindow w : request.blackoutWindows()) {
            if (!end.isBefore(w.from()) && !w.to().isBefore(start)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHardReject(ConflictResponse c) {
        if (HARD_REJECT_TYPES.contains(c.type())) {
            return true;
        }
        return c.type() == ConflictType.CONSTRAINT_VIOLATION
                && c.severity() == ConflictSeverity.HIGH;
    }

    private int score(MaintenanceTask task, LocalDate start, LocalDate end,
                      OptimizationRunRequest request, int softConflicts) {
        int daysEarly = (int) ChronoUnit.DAYS.between(end, request.windowEnd());
        if (daysEarly < 0) {
            daysEarly = 0;
        }
        return priorityWeight(task.getPriority()) + DAYS_EARLY_WEIGHT * daysEarly
                - SOFT_CONFLICT_PENALTY * softConflicts;
    }

    private BlockRequest persistCandidate(MaintenanceTask task, Candidate best) {
        BlockRequest block = new BlockRequest(best.blockCode(), task, task.getCorridor(),
                best.start(), best.end(), persistBlockStatus,
                "Created by optimizer run (algorithm " + algorithm + ")");
        return blockRequestRepository.save(block);
    }

    // ------------------------------------------------------------------
    // Result building (explainability)
    // ------------------------------------------------------------------

    private OptimizationResult scheduled(MaintenanceTask task, Candidate best,
                                         Long blockId, String blockCode) {
        return new OptimizationResult(task.getId(), task.getTaskCode(), task.getTitle(),
                task.getPriority().name(), OptimizationDecision.SCHEDULED,
                "Feasible candidate selected (score " + best.score() + ")",
                best.score(), blockId, blockCode, best.start(), best.end(),
                toJson(best.conflicts()));
    }

    private OptimizationResult rejected(MaintenanceTask task, String reason,
                                        Integer score, List<ConflictResponse> conflicts) {
        return new OptimizationResult(task.getId(), task.getTaskCode(), task.getTitle(),
                task.getPriority().name(), OptimizationDecision.REJECTED,
                reason, score, null, null, null, null, toJson(conflicts));
    }

    private List<String> reasonStrings(List<Candidate> evaluated) {
        List<String> reasons = new ArrayList<>();
        for (Candidate c : evaluated) {
            if (!c.feasible()) {
                reasons.add(c.blockCode() + " [" + c.start() + ".." + c.end()
                        + "]: " + c.reason());
            }
        }
        return reasons;
    }

    private String summaryReason(OptimizationRun run, int rejectedCandidates) {
        return "Algorithm " + algorithm + " scheduled " + run.getTasksScheduled()
                + " task(s), rejected " + run.getTasksRejected() + " task(s), evaluated "
                + run.getCandidatesEvaluated() + " candidate(s) (" + rejectedCandidates
                + " rejected), detected " + run.getConflictsDetected() + " conflict(s);"
                + " objective score " + run.getObjectiveScore();
    }

    // ------------------------------------------------------------------
    // Deterministic helpers
    // ------------------------------------------------------------------

    private static Comparator<MaintenanceTask> taskRank() {
        // Priority weight desc, then duration desc, then task code asc. The duration
        // reversal must be scoped to the duration comparator only, otherwise it would
        // reverse the whole accumulated comparator (making LOW tasks rank first).
        return Comparator
                .comparingInt((MaintenanceTask t) -> priorityWeight(t.getPriority())).reversed()
                .thenComparing(Comparator.comparingInt(MaintenanceTask::getDurationMinutes).reversed())
                .thenComparing(MaintenanceTask::getTaskCode);
    }

    private static int priorityWeight(Priority priority) {
        return switch (priority) {
            case CRITICAL -> PRIORITY_CRITICAL;
            case HIGH -> PRIORITY_HIGH;
            case MEDIUM -> PRIORITY_MEDIUM;
            case LOW -> PRIORITY_LOW;
        };
    }

    private int blockLengthDays(MaintenanceTask task) {
        int length = (task.getDurationMinutes() + maxDailyWorkMinutes - 1) / maxDailyWorkMinutes;
        return Math.max(1, length);
    }

    private String candidateCode(MaintenanceTask task, LocalDate start) {
        String base = "OPT-" + task.getTaskCode() + "-" + start;
        if (!blockRequestRepository.existsByBlockCode(base)) {
            return base;
        }
        // Deterministic fallback when a block with the base code already exists.
        int suffix = 2;
        while (blockRequestRepository.existsByBlockCode(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    // ------------------------------------------------------------------
    // Request validation
    // ------------------------------------------------------------------

    private void validateRequest(OptimizationRunRequest request) {
        if (request.windowEnd().isBefore(request.windowStart())) {
            throw new BadRequestException("windowEnd must be after or equal to windowStart");
        }
        if (request.maxConcurrentBlocks() != null && request.maxConcurrentBlocks() < 1) {
            throw new BadRequestException("maxConcurrentBlocks must be >= 1");
        }
        List<Long> taskIds = request.taskIds();
        if (taskIds != null) {
            for (Long taskId : taskIds) {
                if (taskId == null || !taskRepository.existsById(taskId)) {
                    throw new BadRequestException("Unknown task id: " + taskId);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Persistence access + response mapping
    // ------------------------------------------------------------------

    private OptimizationRun getRunEntity(Long id) {
        return runRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Optimization run not found: " + id));
    }

    private OptimizationRunResponse toSummary(OptimizationRun run) {
        return new OptimizationRunResponse(run.getId(), run.getAlgorithm(), run.getStatus(),
                run.getWindowStart(), run.getWindowEnd(), run.getObjectiveScore(),
                run.getTasksScheduled(), run.getTasksRejected(), run.getConflictsDetected(),
                run.getCandidatesEvaluated(), run.getSummaryReason(), run.getCreatedAt());
    }

    private OptimizationRunDetailResponse toDetail(OptimizationRun run) {
        List<OptimizationResultResponse> results = run.getResults().stream()
                .sorted(Comparator.comparing(OptimizationResult::getId))
                .map(this::toResultResponse)
                .toList();
        return new OptimizationRunDetailResponse(run.getId(), run.getAlgorithm(), run.getStatus(),
                run.getWindowStart(), run.getWindowEnd(), run.getObjectiveScore(),
                run.getTasksScheduled(), run.getTasksRejected(), run.getConflictsDetected(),
                run.getCandidatesEvaluated(), run.getSummaryReason(),
                fromJsonList(run.getRejectedCandidatesJson()), run.getCreatedAt(), results);
    }

    private OptimizationResultResponse toResultResponse(OptimizationResult r) {
        return new OptimizationResultResponse(r.getId(), r.getTaskId(), r.getTaskCode(),
                r.getTaskTitle(), r.getPriority(), r.getDecision(), r.getReason(), r.getScore(),
                r.getBlockId(), r.getBlockCode(), r.getScheduledStart(), r.getScheduledEnd(),
                fromJsonConflicts(r.getConflictsJson()));
    }

    // ------------------------------------------------------------------
    // JSON (de)serialization of explainability payloads
    // ------------------------------------------------------------------

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize optimization data", e);
        }
    }

    private List<ConflictResponse> fromJsonConflicts(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ConflictResponse>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse conflicts JSON", e);
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse rejected-candidates JSON", e);
        }
    }

    // ------------------------------------------------------------------
    // Candidate value type
    // ------------------------------------------------------------------

    /**
     * An evaluated candidate block. {@code reason} is null when the candidate is
     * feasible; otherwise it holds the deterministic reason it was rejected.
     */
    private record Candidate(String blockCode, LocalDate start, LocalDate end, int score,
                             String reason, List<ConflictResponse> conflicts) {

        boolean feasible() {
            return reason == null;
        }

        /** Best candidate: feasible first, higher score, earlier start, code asc. */
        boolean betterThan(Candidate other) {
            if (feasible() != other.feasible()) {
                return feasible();
            }
            if (score != other.score) {
                return score > other.score;
            }
            if (!start.equals(other.start)) {
                return start.isBefore(other.start);
            }
            return blockCode.compareTo(other.blockCode) < 0;
        }
    }
}
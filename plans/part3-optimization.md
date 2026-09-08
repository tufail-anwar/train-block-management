# Part 3 — Maintenance Block Optimization Module Plan

## Goal
Add a deterministic, explainable **Optimization Service** on top of the existing
Part 1 data layer and Part 2 conflict detection. It generates feasible candidate
maintenance blocks from maintenance tasks, train schedules, corridor constraints,
resources and detected conflicts, ranks them, persists run/result data and exposes
REST APIs — with **no AI/ML** and no frontend.

## Reuse
- Entities: `MaintenanceTask`, `BlockRequest`, `Corridor`, `TrainSchedule`,
  `MaintenanceResource`, `Constraint`, `Asset`, `Department`.
- Conflict detection: `ConflictDetectionService.checkProposed(...)` (what-if)
  is the single feasibility oracle; its `ConflictResponse` objects are stored
  as JSON inside `OptimizationResult`.
- Exceptions, validation, `GlobalExceptionHandler`, DTO conventions.

## Deterministic algorithm (`deterministic-greedy-v1`)
1. **Load** eligible tasks (status `DRAFT`/`REQUESTED`/`APPROVED`, or an explicit
   `taskIds` list) whose allowed window overlaps the optimization window.
2. **Rank** tasks by priority weight (CRITICAL 100 > HIGH 60 > MEDIUM 30 > LOW 10),
   then duration, then task code — stable, deterministic order.
3. **Candidate generation** per task: block length = `max(1, ceil(durationMinutes /
   maxDailyWorkMinutes))` days; every start date in the clipped allowed window.
4. **Feasibility** per candidate:
   - hard reject types: `TRAIN_OVERLAP`, `BLOCK_OVERLAP`, `SPATIAL_OVERLAP`,
     `RESOURCE_CONFLICT`, `DURATION_FIT`, `CONSTRAINT_VIOLATION` severity `HIGH`
     (window / corridor not OPERATIONAL / safety rule);
   - soft flags (penalize only): `INSUFFICIENT_GAP`, `CONSTRAINT_VIOLATION`
     severity `MEDIUM` (unmet dependency);
   - direct optimizer constraints: blackout windows (hard), maximum concurrent
     blocks per day (hard).
5. **Scoring** = priority weight + `5 ×` days-early (relative to window end)
   − `15 ×` soft conflict count. Tie-break: earliest start, then candidate code.
6. **Persistence**: selected candidates become `BlockRequest`s (status `DRAFT`
   during the run so later candidates see them, then `SCHEDULED` by default —
   `APPROVED` if configured). Runs and per-task results are stored in
   `optimization_runs` / `optimization_results`.
7. **Explainability**: every run stores `algorithm`, `objectiveScore`, counts,
   a summary reason, per-task `decision`, `reason`, `score`, `conflicts` (JSON)
   and all rejected candidate date ranges with reasons (JSON).

## API
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/optimization/run` | Run optimization for a window; returns run + detailed results |
| `GET` | `/api/optimization/runs/{id}` | Run summary (404 if missing) |
| `GET` | `/api/optimization/runs/{id}/results` | Per-task result list (404 if run missing) |

Request body: `windowStart`, `windowEnd`, optional `taskIds`, optional
`maxConcurrentBlocks`, optional `blackoutWindows[{from,to}]`, optional
`persistBlocks` (default true).

## Files to create
```
plans/part3-optimization.md                                (this plan)
backend/src/main/java/com/ps27/railway/enums/OptimizationStatus.java
backend/src/main/java/com/ps27/railway/enums/OptimizationDecision.java
backend/src/main/java/com/ps27/railway/entity/OptimizationRun.java
backend/src/main/java/com/ps27/railway/entity/OptimizationResult.java
backend/src/main/java/com/ps27/railway/repository/OptimizationRunRepository.java
backend/src/main/java/com/ps27/railway/repository/OptimizationResultRepository.java
backend/src/main/java/com/ps27/railway/dto/OptimizationRunRequest.java
backend/src/main/java/com/ps27/railway/dto/OptimizationRunResponse.java
backend/src/main/java/com/ps27/railway/dto/OptimizationResultResponse.java
backend/src/main/java/com/ps27/railway/dto/OptimizationRunDetailResponse.java
backend/src/main/java/com/ps27/railway/service/OptimizationService.java
backend/src/main/java/com/ps27/railway/controller/OptimizationController.java
backend/src/test/java/com/ps27/railway/service/OptimizationServiceTest.java
backend/src/test/java/com/ps27/railway/controller/OptimizationControllerTest.java
```

## Files to modify
```
backend/src/main/java/com/ps27/railway/service/ConflictDetectionService.java
    - additive public helper hasActiveBlockForTask(Long taskId) (reuses ACTIVE_STATUSES)
backend/src/main/java/com/ps27/railway/config/SampleDataSeeder.java
    - add a few REQUESTED tasks without blocks so the optimizer has something to schedule
backend/src/main/resources/application.yml (+ application-postgres.yml)
    - optimization.max-concurrent-blocks (OPTIMIZATION_MAX_CONCURRENT_BLOCKS, default 5)
    - optimization.persist-block-status (OPTIMIZATION_PERSIST_BLOCK_STATUS, default SCHEDULED)
    - optimization.algorithm (deterministic-greedy-v1)
backend/.env.example
    - commented placeholders for the two new env vars
backend/README.md
    - document the three optimization endpoints + algorithm summary
```

## Tests
- `OptimizationServiceTest` (`@SpringBootTest @Transactional`):
  feasible task scheduled, train-overlap task rejected, max-concurrent-block
  respect, blackout respect, task-with-existing-block skip, critical-priority
  ranking, deterministic repeatability, reversed window / unknown task → 400,
  missing run → 404.
- `OptimizationControllerTest` (`@SpringBootTest @AutoConfigureMockMvc @Transactional`):
  POST run → 200 with results, GET run, GET results, 404 for missing run,
  400 for reversed window / unknown task.

## Verification workflow
1. Implement in small batches (enums → entities/repos → DTOs → service →
   controller → config/seeder → README → tests).
2. `mvn test` (Maven at `%TEMP%\maven-dist\apache-maven-3.9.9`) — fix failures.
3. Run app; verify `POST /api/optimization/run`, `GET /api/optimization/runs/{id}`,
   `GET /api/optimization/runs/{id}/results`.
4. Final summary; stop before Part 4 (Frontend) / Part 6 (AI-ML).

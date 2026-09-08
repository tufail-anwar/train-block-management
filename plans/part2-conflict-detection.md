# Part 2 — Conflict Detection Module Plan

## Goal
Add a deterministic, explainable **Conflict Detection** module on top of the existing
Part 1 data layer. It evaluates proposed/active maintenance blocks against existing
railway operations and returns a list of detected conflicts with type, severity,
affected entities, time/location info and a human-readable explanation.

Constraints honored:
- Reuse the existing entities/repositories/DTOs — **no duplicate concepts**.
- **No optimization** (Part 5), **no AI/ML** (Part 6), **no frontend** (Part 3).
- Detection is **read-only and deterministic** — no AI, no randomness.
- Stays on the existing `Controller -> Service -> Repository -> Database` architecture.

## Conflict types (from project docs hard constraints + user requirements)

| ConflictType | Source | Rule (deterministic) |
|---|---|---|
| `TRAIN_OVERLAP` | docs hard constraint "No prohibited train overlap" | An active block's date range overlaps a `TrainSchedule` on the same corridor (explicit `scheduleDate`, or recurring `dayOfWeek` within range) |
| `BLOCK_OVERLAP` | user req "Overlapping maintenance blocks" | Two active blocks on the same corridor with overlapping date ranges |
| `SPATIAL_OVERLAP` | user req "Corridor/spatial overlap" | Two overlapping blocks whose tasks target the **same asset** (same signal/bridge can't have two blocks at once) |
| `INSUFFICIENT_GAP` | user req "Insufficient gap between blocks" | Consecutive blocks on the same corridor (sorted by start) with gap < configured `minGapDays` (default 1 day) |
| `RESOURCE_CONFLICT` | docs hard constraint "Resource capacity must not be exceeded" | Per department/day: number of active blocks whose task's department exceeds that department's combined `capacityPerShift` of active resources |
| `CONSTRAINT_VIOLATION` | docs hard constraint "Configured operational/safety rules must be respected" | Block outside its task's `[earliestStart, latestEnd]` allowed window; corridor not `OPERATIONAL`; an unmet dependency (dependency task not `COMPLETED`); active `SAFETY_RULE`/`OPERATIONAL_RULE` constraint on the task |
| `DURATION_FIT` | docs hard constraint "Required task duration must fit" | A single-day block where `durationMinutes` exceeds configured `maxDailyWorkMinutes` (default 720) |

Active blocks = `BlockRequestStatus` in `{DRAFT, REQUESTED, APPROVED, SCHEDULED}`.
`REJECTED`/`CANCELLED` blocks are ignored.

## Severity (deterministic, explainable)
- `TRAIN_OVERLAP`: `HIGH` if train is `PASSENGER`/`SUBURBAN`, else `MEDIUM`
- `BLOCK_OVERLAP` / `SPATIAL_OVERLAP`: `HIGH` if either task priority is `CRITICAL`, else `MEDIUM`
- `INSUFFICIENT_GAP`: `LOW`
- `RESOURCE_CONFLICT`: `MEDIUM`
- `CONSTRAINT_VIOLATION`: `HIGH` (window/corridor/safety), `MEDIUM` (dependency)
- `DURATION_FIT`: `HIGH`

## API
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/conflicts` | Detect conflicts for all active blocks; optional `corridorId`, `from`, `to` filters |
| `GET` | `/api/conflicts/{blockId}` | Detect conflicts for one block (404 if missing) |
| `POST` | `/api/conflicts/check` | What-if check: validate a proposed block request (`BlockRequestRequest` body) **without persisting**; returns conflicts |

Validation/error handling reused from Part 1 (`@Valid`, `BadRequestException`,
`ResourceNotFoundException`, `GlobalExceptionHandler` → consistent `ApiError`).
Missing/invalid filters return 400; unknown block/corridor return 404.

## Files to create
```
plans/part2-conflict-detection.md                              (this plan)
backend/src/main/java/com/ps27/railway/enums/ConflictType.java
backend/src/main/java/com/ps27/railway/enums/ConflictSeverity.java
backend/src/main/java/com/ps27/railway/dto/ConflictResponse.java
backend/src/main/java/com/ps27/railway/dto/ConflictCheckResponse.java
backend/src/main/java/com/ps27/railway/service/ConflictDetectionService.java
backend/src/main/java/com/ps27/railway/controller/ConflictController.java
backend/src/test/java/com/ps27/railway/service/ConflictDetectionServiceTest.java
backend/src/test/java/com/ps27/railway/controller/ConflictControllerTest.java
```

## Files to modify
```
backend/src/main/java/com/ps27/railway/config/SampleDataSeeder.java
    - add synthetic conflict scenarios (overlapping blocks on COR-001,
      a train schedule overlapping a block day, same-asset overlap,
      a department capacity clash, a window violation)
backend/README.md
    - document the three conflict endpoints + conflict types table
backend/src/main/resources/application.yml  (+ application-postgres.yml)
    - configurable CONFLICT_MIN_GAP_DAYS (default 1)
    - configurable CONFLICT_MAX_DAILY_WORK_MINUTES (default 720)
backend/.env.example
    - commented placeholders for the two new env vars
```

## Detection engine shape (`ConflictDetectionService`)
- `List<ConflictResponse> detectAll(Long corridorId, LocalDate from, LocalDate to)`
  — loads active blocks (optionally filtered), runs all seven checks, returns a
  deterministic, sorted list.
- `List<ConflictResponse> detectForBlock(Long blockId)` — checks one block against
  the rest of the dataset.
- `List<ConflictResponse> checkProposed(BlockRequestRequest request)` — what-if;
  loads referenced task/corridor, validates dates, runs checks against existing
  active blocks + schedules + constraints without persisting.
- Internal helpers per check; every result carries a plain-English `explanation`
  (e.g. "Block BLK-2026-001 (Track geometry correction) overlaps train TRN-101
  North Express on corridor COR-001 on 2026-09-10").

## Tests
- `ConflictDetectionServiceTest` (`@SpringBootTest @Transactional`):
  train overlap, block overlap, spatial (same asset), insufficient gap,
  resource capacity, window/constraint violation, dependency, duration fit,
  what-if check for a clean block, deterministic ordering.
- `ConflictControllerTest` (`@SpringBootTest @AutoConfigureMockMvc @Transactional`):
  GET all (with filters), GET by block id, 404 for missing block, POST check
  returns conflicts for a conflicting proposed block and none for a clean one,
  invalid filter → 400.

## Verification workflow
1. Implement in small batches (enums → DTOs → service → controller → seeder → tests).
2. `mvn test` (local Maven at `%TEMP%\maven-dist\apache-maven-3.9.9`) — fix failures.
3. Run app on default profile; verify `GET /api/conflicts` returns realistic conflicts
   from the enhanced synthetic seed data.
4. Final summary; **stop before Part 3 / Optimization**.

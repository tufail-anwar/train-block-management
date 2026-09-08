# PS27 – Railway Maintenance Block Optimizer (Part 1 + Conflict Detection + Optimization)

Spring Boot backend for managing railway maintenance planning data. Part 1 builds
the data layer and CRUD APIs; the **conflict detection module** (Part 4 docs)
adds deterministic, explainable conflict checks on top of the existing data;
the **block optimization module** (Part 5 docs) adds a deterministic, explainable
optimizer that generates feasible maintenance block plans from tasks, schedules,
constraints and resources — **no AI/ML**.

## Tech stack

- Java 17, Spring Boot 3.3, Maven
- Spring Data JPA
- H2 (PostgreSQL compatibility mode) for local development and tests
- PostgreSQL driver (profile `postgres`) for later Supabase deployment

## Layered architecture

```
Controller (DTO in/out)
    ↓
Service (business rules, validation, mapping)
    ↓
Repository (Spring Data JPA)
    ↓
Database (H2 local / PostgreSQL cloud)
```

## Run locally (H2 in-memory, synthetic sample data)

```bash
cd backend
mvn spring-boot:run
```

The default profile uses an in-memory H2 database (PostgreSQL mode) and seeds
synthetic sample data on startup. No database credentials are required.

Smoke test:

```bash
curl http://localhost:8080/api/corridors
```

## Run with PostgreSQL (for later Supabase deployment)

Set environment variables (see `.env.example` — placeholders only, never commit
real credentials), then:

```bash
cd backend
export DB_URL=jdbc:postgresql://<host>:<port>/<db>
export DB_USERNAME=<user>
export DB_PASSWORD=<password>
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Tests

```bash
cd backend
mvn test
```

## Main APIs (Part 1)

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/api/maintenance` | List / create maintenance tasks |
| GET/PUT/DELETE | `/api/maintenance/{id}` | Read / update / delete maintenance task |
| POST | `/api/maintenance/import` | Import synthetic maintenance tasks from CSV (multipart) |
| GET/POST | `/api/block-requests` | List / create block requests (maintenance blocks) |
| GET/PUT/DELETE | `/api/block-requests/{id}` | Read / update / delete block request |
| GET/POST | `/api/corridors` | List / create corridors |
| GET/PUT/DELETE | `/api/corridors/{id}` | Read / update / delete corridor |
| GET | `/api/corridors/{id}/availability` | Corridor availability (read-only listing, no conflict logic) |
| GET/POST | `/api/assets` | List / create railway assets |
| GET/PUT/DELETE | `/api/assets/{id}` | Read / update / delete asset |
| GET/POST | `/api/trains` | List / create trains |
| GET/PUT/DELETE | `/api/trains/{id}` | Read / update / delete train |
| GET/POST | `/api/train-schedules` | List / create train schedules |
| GET/PUT/DELETE | `/api/train-schedules/{id}` | Read / update / delete train schedule |
| GET/POST | `/api/resources` | List / create maintenance resources |
| GET/PUT/DELETE | `/api/resources/{id}` | Read / update / delete resource |
| GET/POST | `/api/departments` | List / create departments |
| GET/PUT/DELETE | `/api/departments/{id}` | Read / update / delete department |
| GET/POST | `/api/users` | List / create users |
| GET/PUT/DELETE | `/api/users/{id}` | Read / update / delete user |
| GET/POST | `/api/constraints` | List / create constraints |
| GET/PUT/DELETE | `/api/constraints/{id}` | Read / update / delete constraint |

## Block optimization (Part 5)

Deterministic, explainable optimizer (`deterministic-greedy-v1`, no AI/ML, no
randomness — identical input produces identical output). It ranks eligible tasks
(`DRAFT`/`REQUESTED`/`APPROVED`) by priority weight (CRITICAL 100 > HIGH 60 >
MEDIUM 30 > LOW 10), then duration, then task code. For each task it enumerates
every candidate start date in the clipped allowed window with block length
`max(1, ceil(durationMinutes / maxDailyWorkMinutes))` days and reuses
`ConflictDetectionService.checkProposed(...)` as the feasibility oracle.

| Method | Path | Description |
|---|---|---|
| POST | `/api/optimization/run` | Run optimization for a window; returns run + detailed per-task results (persisted) |
| GET | `/api/optimization/runs/{id}` | Run summary (404 if missing) |
| GET | `/api/optimization/runs/{id}/results` | Per-task result list (404 if run missing) |

Request body:

```json
{
  "windowStart": "2026-12-01",
  "windowEnd": "2026-12-15",
  "taskIds": [1, 2],
  "maxConcurrentBlocks": 5,
  "blackoutWindows": [{"from": "2026-12-05", "to": "2026-12-06"}],
  "persistBlocks": true
}
```

`windowStart`/`windowEnd` are required; `taskIds`, `maxConcurrentBlocks`,
`blackoutWindows` and `persistBlocks` (default `true`) are optional. Feasible
candidates are persisted as `BlockRequest`s (status `SCHEDULED` by default);
runs and per-task decisions are stored in `optimization_runs` /
`optimization_results` with JSON explainability payloads (conflicts and rejected
candidate ranges with reasons).

Hard rejects: `TRAIN_OVERLAP`, `BLOCK_OVERLAP`, `SPATIAL_OVERLAP`,
`RESOURCE_CONFLICT`, `DURATION_FIT`, `CONSTRAINT_VIOLATION` HIGH, blackout
overlap, exceeding the per-day concurrent-block limit. Soft flags (penalize
only): `INSUFFICIENT_GAP`, `CONSTRAINT_VIOLATION` MEDIUM. Score = priority
weight + `5 ×` days-early − `15 ×` soft-conflicts; tie-break earliest start,
then candidate code.

Tuning (environment-overridable):

```yaml
optimization:
  algorithm: deterministic-greedy-v1     # OPTIMIZATION_ALGORITHM
  max-concurrent-blocks: 5               # OPTIMIZATION_MAX_CONCURRENT_BLOCKS
  persist-block-status: SCHEDULED        # OPTIMIZATION_PERSIST_BLOCK_STATUS
```

Smoke test after `mvn spring-boot:run`:

```bash
curl -X POST http://localhost:8080/api/optimization/run \
  -H 'Content-Type: application/json' \
  -d '{"windowStart":"2026-12-01","windowEnd":"2026-12-15"}'
curl http://localhost:8080/api/optimization/runs/1
curl http://localhost:8080/api/optimization/runs/1/results
```

## Conflict detection (Part 4)

Read-only, deterministic checks over active blocks (`DRAFT`, `REQUESTED`,
`APPROVED`, `SCHEDULED`). `REJECTED` / `CANCELLED` blocks are ignored.

| Method | Path | Description |
|---|---|---|
| GET | `/api/conflicts` | Detect conflicts for all active blocks; optional `corridorId`, `from`, `to` filters |
| GET | `/api/conflicts/{blockId}` | Detect conflicts for one block (404 if missing) |
| POST | `/api/conflicts/check` | What-if check of a proposed `BlockRequestRequest` body — validated **without persisting**; returns conflicts (empty when clean) |

Conflict types and severities:

| ConflictType | Meaning | Severity |
|---|---|---|
| `TRAIN_OVERLAP` | Block overlaps a train schedule on the same corridor | HIGH (PASSENGER/SUBURBAN), else MEDIUM |
| `BLOCK_OVERLAP` | Two active blocks overlap on the same corridor | HIGH if critical task, else MEDIUM |
| `SPATIAL_OVERLAP` | Two overlapping blocks target the same asset | HIGH if critical task, else MEDIUM |
| `INSUFFICIENT_GAP` | Free days between consecutive corridor blocks < `min-gap-days` | LOW |
| `RESOURCE_CONFLICT` | Blocks of a department exceed combined resource capacity on a day | MEDIUM |
| `CONSTRAINT_VIOLATION` | Outside task window / corridor not OPERATIONAL / unmet dependency / active safety-rule | HIGH (window, corridor, safety) / MEDIUM (dependency) |
| `DURATION_FIT` | Single-day block exceeds the configured daily work limit | HIGH |

Tuning (environment-overridable):

```yaml
conflict:
  min-gap-days: 1                     # CONFLICT_MIN_GAP_DAYS
  max-daily-work-minutes: 720         # CONFLICT_MAX_DAILY_WORK_MINUTES
```

Smoke test after `mvn spring-boot:run`:

```bash
curl http://localhost:8080/api/conflicts
curl http://localhost:8080/api/conflicts?corridorId=1
curl -X POST http://localhost:8080/api/conflicts/check \
  -H 'Content-Type: application/json' \
  -d '{"blockCode":"BLK-WHATIF","maintenanceTaskId":1,"corridorId":1,"requestedStart":"2026-12-01","requestedEnd":"2026-12-02"}'
```

## Sample data

- `SampleDataSeeder` (dev profile only) seeds synthetic departments, corridors,
  assets, trains, schedules, resources, users, constraints, tasks and block
  requests, including conflict scenarios (train overlap, block/spatial overlap,
  department capacity clash, window violation, duration fit). **No real railway
  operational data is used.**
- `src/main/resources/sample-data/maintenance-tasks-sample.csv` contains synthetic
  tasks for the CSV import endpoint.

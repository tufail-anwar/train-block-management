# PS27 – Railway Maintenance Block Optimizer (Part 1 + Conflict Detection)

Spring Boot backend for managing railway maintenance planning data. Part 1 builds
the data layer and CRUD APIs; the **conflict detection module** (Part 4 docs)
adds deterministic, explainable conflict checks on top of the existing data.
Optimization, block plans, audit, security, AI/ML, Firebase and the frontend are
still out of scope and deferred to later parts.

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

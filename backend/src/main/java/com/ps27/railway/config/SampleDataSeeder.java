package com.ps27.railway.config;

import com.ps27.railway.entity.Asset;
import com.ps27.railway.entity.BlockRequest;
import com.ps27.railway.entity.Constraint;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.Department;
import com.ps27.railway.entity.MaintenanceResource;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.entity.Train;
import com.ps27.railway.entity.TrainSchedule;
import com.ps27.railway.entity.User;
import com.ps27.railway.enums.AssetStatus;
import com.ps27.railway.enums.AssetType;
import com.ps27.railway.enums.BlockRequestStatus;
import com.ps27.railway.enums.ConstraintType;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.enums.Direction;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.ResourceType;
import com.ps27.railway.enums.Role;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
import com.ps27.railway.enums.TrainType;
import com.ps27.railway.repository.AssetRepository;
import com.ps27.railway.repository.BlockRequestRepository;
import com.ps27.railway.repository.ConstraintRepository;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.DepartmentRepository;
import com.ps27.railway.repository.MaintenanceResourceRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import com.ps27.railway.repository.TrainRepository;
import com.ps27.railway.repository.TrainScheduleRepository;
import com.ps27.railway.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Seeds synthetic sample data for local development and testing.
 * Active only on the default profile; never enabled in production.
 * All data is fictional — no real railway operational data is used.
 */
@Component
@Profile("!postgres & !test")
public class SampleDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataSeeder.class);

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CorridorRepository corridorRepository;
    private final AssetRepository assetRepository;
    private final TrainRepository trainRepository;
    private final TrainScheduleRepository trainScheduleRepository;
    private final MaintenanceTaskRepository taskRepository;
    private final MaintenanceResourceRepository resourceRepository;
    private final ConstraintRepository constraintRepository;
    private final BlockRequestRepository blockRequestRepository;

    public SampleDataSeeder(DepartmentRepository departmentRepository,
                            UserRepository userRepository,
                            CorridorRepository corridorRepository,
                            AssetRepository assetRepository,
                            TrainRepository trainRepository,
                            TrainScheduleRepository trainScheduleRepository,
                            MaintenanceTaskRepository taskRepository,
                            MaintenanceResourceRepository resourceRepository,
                            ConstraintRepository constraintRepository,
                            BlockRequestRepository blockRequestRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.corridorRepository = corridorRepository;
        this.assetRepository = assetRepository;
        this.trainRepository = trainRepository;
        this.trainScheduleRepository = trainScheduleRepository;
        this.taskRepository = taskRepository;
        this.resourceRepository = resourceRepository;
        this.constraintRepository = constraintRepository;
        this.blockRequestRepository = blockRequestRepository;
    }

    @Override
    public void run(String... args) {
        if (departmentRepository.count() > 0) {
            log.info("Sample data already present, skipping seeding");
            return;
        }
        log.info("Seeding synthetic sample data...");

        // Departments
        Department track = saveDepartment("DEPT-TRK", "Track Department", "Track and alignment maintenance");
        Department signal = saveDepartment("DEPT-SIG", "Signals Department", "Signalling and interlocking maintenance");
        Department ohe = saveDepartment("DEPT-OHE", "OHE Department", "Overhead electrification maintenance");
        Department eng = saveDepartment("DEPT-ENG", "Civil Engineering", "Bridges, tunnels and structures");
        Department ops = saveDepartment("DEPT-OPS", "Operations", "Train operations coordination");

        // Users (identity/role only - no credentials in Part 1)
        saveUser("admin", "admin@ps27.example", "System Admin", Role.ADMIN, ops);
        saveUser("planner1", "planner1@ps27.example", "Priya Planner", Role.PLANNER, ops);
        saveUser("approver1", "approver1@ps27.example", "Arun Approver", Role.APPROVER, ops);
        saveUser("track.user", "track.user@ps27.example", "Ravi Track", Role.DEPARTMENT_USER, track);

        // Corridors
        Corridor cor1 = saveCorridor("COR-001", "North Corridor", "Station North", "City Central", 120.0, CorridorStatus.OPERATIONAL);
        Corridor cor2 = saveCorridor("COR-002", "East Corridor", "City Central", "Port Junction", 85.0, CorridorStatus.OPERATIONAL);
        Corridor cor3 = saveCorridor("COR-003", "Hill Corridor", "City Central", "Hill Station", 160.0, CorridorStatus.PARTIALLY_BLOCKED);

        // Assets
        saveAsset("AST-001", "Track Section km 45-48", AssetType.TRACK_SECTION, cor1, 46.5, AssetStatus.MAINTENANCE_REQUIRED);
        saveAsset("AST-002", "Bridge BR-17", AssetType.BRIDGE, cor1, 61.0, AssetStatus.OPERATIONAL);
        saveAsset("AST-003", "Signal S-102", AssetType.SIGNAL, cor2, 32.0, AssetStatus.MAINTENANCE_REQUIRED);
        saveAsset("AST-004", "Tunnel T-3", AssetType.TUNNEL, cor3, 103.0, AssetStatus.OPERATIONAL);
        saveAsset("AST-005", "Level Crossing LC-4", AssetType.LEVEL_CROSSING, cor3, 55.0, AssetStatus.OPERATIONAL);

        // Trains
        Train t1 = saveTrain("TRN-101", "North Express", TrainType.PASSENGER, true);
        Train t2 = saveTrain("TRN-202", "Coastal Freight", TrainType.FREIGHT, true);
        Train t3 = saveTrain("TRN-303", "Hill Local", TrainType.SUBURBAN, true);
        Train t4 = saveTrain("TRN-404", "Goods Runner", TrainType.GOODS_LOCAL, true);

        // Train schedules (synthetic, fixed future dates)
        LocalDate start = LocalDate.now().plusDays(1);
        saveSchedule(t1, cor1, start, DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(8, 30), Direction.UP);
        saveSchedule(t1, cor1, start.plusDays(1), DayOfWeek.TUESDAY, LocalTime.of(6, 0), LocalTime.of(8, 30), Direction.UP);
        saveSchedule(t2, cor1, start, DayOfWeek.MONDAY, LocalTime.of(22, 0), LocalTime.of(23, 45), Direction.DOWN);
        saveSchedule(t3, cor3, start.plusDays(1), DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 15), Direction.UP);
        saveSchedule(t4, cor2, start, DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(14, 40), Direction.DOWN);

        // Maintenance tasks (synthetic)
        MaintenanceTask task1 = saveTask("TRK-2026-001", "Track geometry correction km 45-48",
                "Correct track alignment defects on section km 45-48", TaskType.TRACK_RELAY,
                Priority.HIGH, 480, cor1, assetByCode("AST-001"), track,
                start.plusDays(2), start.plusDays(4), start.plusDays(1), start.plusDays(10),
                TaskStatus.REQUESTED, "Requires night block");
        MaintenanceTask task2 = saveTask("BRG-2026-002", "Bridge bearing replacement BR-17",
                "Replace worn bearings on bridge BR-17", TaskType.REPLACEMENT,
                Priority.CRITICAL, 300, cor1, assetByCode("AST-002"), eng,
                start.plusDays(6), start.plusDays(8), start.plusDays(5), start.plusDays(15),
                TaskStatus.REQUESTED, null);
        MaintenanceTask task3 = saveTask("SIG-2026-003", "Signal head replacement S-102",
                "Replace defective signal head at km 32", TaskType.SIGNAL_UPGRADE,
                Priority.HIGH, 180, cor2, assetByCode("AST-003"), signal,
                start.plusDays(3), start.plusDays(3), start.plusDays(2), start.plusDays(8),
                TaskStatus.REQUESTED, null);
        MaintenanceTask task4 = saveTask("TNL-2026-004", "Tunnel lining inspection T-3",
                "Visual inspection of tunnel lining", TaskType.INSPECTION,
                Priority.MEDIUM, 480, cor3, assetByCode("AST-004"), eng,
                start.plusDays(5), start.plusDays(7), start.plusDays(4), start.plusDays(12),
                TaskStatus.DRAFT, null);

        // Task dependency: task2 (bridge) depends on task1 (track work on same corridor)
        task2.setDependencies(Set.of(task1));
        taskRepository.save(task2);

        // Maintenance resources
        saveResource("RES-TRK-01", "Track Crew Alpha", ResourceType.CREW, track, 12, true);
        saveResource("RES-TRK-02", "Track Maintenance Machine", ResourceType.MACHINERY, track, 1, true);
        saveResource("RES-SIG-01", "Signal Crew Beta", ResourceType.CREW, signal, 6, true);
        saveResource("RES-ENG-01", "Structural Team", ResourceType.CREW, eng, 8, true);

        // Constraints (maintenance requirements)
        saveConstraint(ConstraintType.TIME_WINDOW, "Night-only window", "Track work only allowed 00:00-05:00", task1, true);
        saveConstraint(ConstraintType.CORRIDOR_AVAILABILITY, "No freight overlap", "Corridor COR-001 closed to freight during block", task2, true);
        saveConstraint(ConstraintType.RESOURCE_CAPACITY, "Single machine limit", "Only one track machine available", task1, true);

        // Block requests
        saveBlockRequest("BLK-2026-001", task1, cor1, start.plusDays(2), start.plusDays(4), BlockRequestStatus.REQUESTED, "Night block on North Corridor");
        saveBlockRequest("BLK-2026-002", task2, cor1, start.plusDays(6), start.plusDays(8), BlockRequestStatus.REQUESTED, "Day block for bridge work");
        saveBlockRequest("BLK-2026-003", task3, cor2, start.plusDays(3), start.plusDays(3), BlockRequestStatus.DRAFT, "Signal replacement");

        // --- Part 2 (conflict detection) synthetic scenarios -------------
        // Train overlap: passenger run on COR-001 on the first block day of BLK-2026-001.
        saveSchedule(t1, cor1, start.plusDays(2), start.plusDays(2).getDayOfWeek(),
                LocalTime.of(7, 0), LocalTime.of(9, 0), Direction.UP);

        // Block + spatial overlap: same corridor and same asset (AST-001) as BLK-2026-001.
        saveBlockRequest("BLK-2026-004", task1, cor1, start.plusDays(3), start.plusDays(5),
                BlockRequestStatus.REQUESTED, "Overlaps BLK-2026-001 in time and asset");

        // Department capacity clash: OHE department has no active resources -> capacity 0.
        MaintenanceTask oheTask = saveTask("OHE-2026-005", "OHE insulator replacement",
                "Replace worn insulators on overhead equipment", TaskType.PREVENTIVE_MAINTENANCE,
                Priority.MEDIUM, 240, cor2, null, ohe,
                start.plusDays(4), start.plusDays(5), start.plusDays(1), start.plusDays(9),
                TaskStatus.REQUESTED, null);
        saveBlockRequest("BLK-2026-005", oheTask, cor2, start.plusDays(4), start.plusDays(5),
                BlockRequestStatus.REQUESTED, "OHE capacity not available");

        // Window violation: block ends after the task's latest allowed end.
        saveBlockRequest("BLK-2026-006", task1, cor1, start.plusDays(9), start.plusDays(12),
                BlockRequestStatus.REQUESTED, "Ends after latest allowed end (window violation)");

        // Duration fit: single-day block whose task exceeds the configured daily work limit.
        MaintenanceTask longTask = saveTask("TRK-2026-006", "Long track renewal shift",
                "Continuous renewal requiring more than one shift", TaskType.TRACK_RELAY,
                Priority.HIGH, 900, cor2, null, track,
                start.plusDays(7), start.plusDays(7), start.plusDays(6), start.plusDays(10),
                TaskStatus.REQUESTED, null);
        saveBlockRequest("BLK-2026-007", longTask, cor2, start.plusDays(7), start.plusDays(7),
                BlockRequestStatus.REQUESTED, "Duration exceeds daily limit");

        log.info("Sample data seeding complete");
    }

    private Department saveDepartment(String code, String name, String description) {
        return departmentRepository.save(new Department(code, name, description));
    }

    private void saveUser(String username, String email, String fullName, Role role, Department department) {
        userRepository.save(new User(username, email, fullName, role, department));
    }

    private Corridor saveCorridor(String code, String name, String start, String end,
                                  double lengthKm, CorridorStatus status) {
        return corridorRepository.save(new Corridor(code, name, start, end, lengthKm, status));
    }

    private void saveAsset(String code, String name, AssetType type, Corridor corridor,
                           double trackKm, AssetStatus status) {
        assetRepository.save(new Asset(code, name, type, corridor, trackKm, status));
    }

    private Asset assetByCode(String code) {
        return assetRepository.findByCode(code).orElse(null);
    }

    private Train saveTrain(String code, String name, TrainType type, boolean active) {
        return trainRepository.save(new Train(code, name, type, active));
    }

    private void saveSchedule(Train train, Corridor corridor, LocalDate date, DayOfWeek dow,
                              LocalTime dep, LocalTime arr, Direction direction) {
        trainScheduleRepository.save(new TrainSchedule(train, corridor, date, dow, dep, arr, direction));
    }

    private MaintenanceTask saveTask(String code, String title, String description, TaskType type,
                                     Priority priority, int duration, Corridor corridor, Asset asset,
                                     Department department, LocalDate reqStart, LocalDate reqEnd,
                                     LocalDate earliest, LocalDate latest, TaskStatus status, String notes) {
        return taskRepository.save(new MaintenanceTask(code, title, description, type, priority,
                duration, corridor, asset, department, reqStart, reqEnd, earliest, latest, status, notes));
    }

    private void saveResource(String code, String name, ResourceType type, Department department,
                              int capacity, boolean active) {
        resourceRepository.save(new MaintenanceResource(code, name, type, department, capacity, active));
    }

    private void saveConstraint(ConstraintType type, String name, String description,
                                MaintenanceTask task, boolean active) {
        constraintRepository.save(new Constraint(type, name, description, task, active));
    }

    private void saveBlockRequest(String code, MaintenanceTask task, Corridor corridor,
                                  LocalDate start, LocalDate end, BlockRequestStatus status, String notes) {
        blockRequestRepository.save(new BlockRequest(code, task, corridor, start, end, status, notes));
    }
}

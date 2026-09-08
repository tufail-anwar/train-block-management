package com.ps27.railway.controller;

import com.ps27.railway.entity.BlockRequest;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.entity.Train;
import com.ps27.railway.entity.TrainSchedule;
import com.ps27.railway.enums.BlockRequestStatus;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.enums.Direction;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
import com.ps27.railway.enums.TrainType;
import com.ps27.railway.repository.BlockRequestRepository;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import com.ps27.railway.repository.TrainRepository;
import com.ps27.railway.repository.TrainScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConflictControllerTest {

    private static final LocalDate D = LocalDate.of(2026, 12, 1); // a Tuesday

    @Autowired
    private MockMvc mockMvc;

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

    private Long corridorId;
    private Long taskId;
    private Long otherTaskId;
    private Long blockId;
    private Long cleanCorridorId;
    private Long cleanTaskId;

    @BeforeEach
    void setUp() {
        blockRequestRepository.deleteAll();
        trainScheduleRepository.deleteAll();
        trainRepository.deleteAll();
        taskRepository.deleteAll();
        corridorRepository.deleteAll();

        Corridor corridor = corridorRepository.save(
                new Corridor("COR-CTRL", "Control Corridor", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
        corridorId = corridor.getId();

        MaintenanceTask task = taskRepository.save(new MaintenanceTask(
                "TASK-CTRL-1", "Control task", "desc", TaskType.INSPECTION, Priority.HIGH, 120,
                corridor, null, null, D, D.plusDays(1),
                null, null, TaskStatus.REQUESTED, null));
        taskId = task.getId();

        MaintenanceTask otherTask = taskRepository.save(new MaintenanceTask(
                "TASK-CTRL-2", "Second task", "desc", TaskType.INSPECTION, Priority.LOW, 60,
                corridor, null, null, D, D.plusDays(1),
                null, null, TaskStatus.REQUESTED, null));
        otherTaskId = otherTask.getId();

        BlockRequest block = blockRequestRepository.save(new BlockRequest(
                "BLK-CTRL-1", task, corridor, D, D, BlockRequestStatus.REQUESTED, null));
        blockId = block.getId();
        // Second block overlapping the first -> BLOCK_OVERLAP.
        blockRequestRepository.save(new BlockRequest(
                "BLK-CTRL-2", otherTask, corridor, D, D.plusDays(1), BlockRequestStatus.REQUESTED, null));

        // Passenger train running on D -> TRAIN_OVERLAP (HIGH).
        Train train = trainRepository.save(
                new Train("TRN-CTRL", "Control Train", TrainType.PASSENGER, true));
        trainScheduleRepository.save(new TrainSchedule(train, corridor, D, null,
                LocalTime.of(6, 0), LocalTime.of(8, 0), Direction.UP));

        // A corridor with no blocks or traffic -> clean what-if target.
        Corridor clean = corridorRepository.save(
                new Corridor("COR-CLEAN", "Clean Corridor", "A", "B", 5.0, CorridorStatus.OPERATIONAL));
        cleanCorridorId = clean.getId();
        MaintenanceTask cleanTask = taskRepository.save(new MaintenanceTask(
                "TASK-CLEAN", "Clean task", "desc", TaskType.INSPECTION, Priority.LOW, 60,
                clean, null, null, D, D.plusDays(1),
                null, null, TaskStatus.REQUESTED, null));
        cleanTaskId = cleanTask.getId();
    }

    @Test
    void findAllReturnsDetectedConflicts() throws Exception {
        // Both blocks are subjects; each reports its own TRAIN_OVERLAP and BLOCK_OVERLAP.
        mockMvc.perform(get("/api/conflicts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[?(@.type == 'TRAIN_OVERLAP')]", hasSize(2)))
                .andExpect(jsonPath("$[?(@.type == 'BLOCK_OVERLAP')]", hasSize(2)))
                .andExpect(jsonPath("$[?(@.type == 'TRAIN_OVERLAP')].severity")
                        .value(org.hamcrest.Matchers.hasItem("HIGH")));
    }

    @Test
    void findAllFiltersByCorridor() throws Exception {
        mockMvc.perform(get("/api/conflicts").param("corridorId", String.valueOf(cleanCorridorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void findAllFiltersByDateRange() throws Exception {
        // A later window (no blocks) yields no conflicts.
        mockMvc.perform(get("/api/conflicts")
                        .param("from", "2026-12-10")
                        .param("to", "2026-12-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void findAllRejectsRangeWithoutTo() throws Exception {
        mockMvc.perform(get("/api/conflicts").param("from", "2026-12-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findByBlockIdReturnsItsConflicts() throws Exception {
        mockMvc.perform(get("/api/conflicts/{blockId}", blockId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.blockId == " + blockId + ")]").exists());
    }

    @Test
    void findByBlockIdMissingReturns404() throws Exception {
        mockMvc.perform(get("/api/conflicts/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkConflictingProposedBlockReturnsConflicts() throws Exception {
        String body = """
                {
                  "blockCode": "BLK-CHK-X",
                  "maintenanceTaskId": %d,
                  "corridorId": %d,
                  "requestedStart": "2026-12-01",
                  "requestedEnd": "2026-12-01"
                }
                """.formatted(taskId, corridorId);

        // Candidate overlaps the passenger train (1) and both existing blocks (2) = 3 conflicts.
        mockMvc.perform(post("/api/conflicts/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockCode").value("BLK-CHK-X"))
                .andExpect(jsonPath("$.conflicts", hasSize(3)))
                .andExpect(jsonPath("$.conflicts[?(@.type == 'TRAIN_OVERLAP')]", hasSize(1)))
                .andExpect(jsonPath("$.conflicts[?(@.type == 'BLOCK_OVERLAP')]", hasSize(2)));
    }

    @Test
    void checkCleanProposedBlockReturnsNoConflicts() throws Exception {
        String body = """
                {
                  "blockCode": "BLK-CHK-CLEAN",
                  "maintenanceTaskId": %d,
                  "corridorId": %d,
                  "requestedStart": "2026-12-01",
                  "requestedEnd": "2026-12-01"
                }
                """.formatted(cleanTaskId, cleanCorridorId);

        mockMvc.perform(post("/api/conflicts/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockCode").value("BLK-CHK-CLEAN"))
                .andExpect(jsonPath("$.conflicts", hasSize(0)));
    }

    @Test
    void checkRejectsUnknownTask() throws Exception {
        String body = """
                {
                  "blockCode": "BLK-CHK-BAD",
                  "maintenanceTaskId": 999999,
                  "corridorId": %d,
                  "requestedStart": "2026-12-01",
                  "requestedEnd": "2026-12-01"
                }
                """.formatted(corridorId);

        mockMvc.perform(post("/api/conflicts/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

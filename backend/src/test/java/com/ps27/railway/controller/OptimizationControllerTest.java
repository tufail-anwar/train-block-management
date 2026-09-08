package com.ps27.railway.controller;

import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
import com.ps27.railway.repository.BlockRequestRepository;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import com.ps27.railway.repository.OptimizationRunRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for the block optimization endpoints (Part 5). Uses a
 * far-future window and explicit {@code taskIds} so the seeded synthetic sample
 * data cannot interfere.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OptimizationControllerTest {

    /** A fixed far-future Tuesday, far away from the seeded sample data. */
    private static final LocalDate D = LocalDate.of(2030, 6, 4);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorridorRepository corridorRepository;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    @Autowired
    private BlockRequestRepository blockRequestRepository;

    @Autowired
    private OptimizationRunRepository runRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainScheduleRepository trainScheduleRepository;

    private Long taskId;
    private Long otherTaskId;

    @BeforeEach
    void setUp() {
        blockRequestRepository.deleteAll();
        trainScheduleRepository.deleteAll();
        trainRepository.deleteAll();
        runRepository.deleteAll();
        taskRepository.deleteAll();
        corridorRepository.deleteAll();

        // Two distinct corridors so the two tasks never block-overlap each other.
        Corridor corridorA = corridorRepository.save(
                new Corridor("COR-OPCTRL-A", "Optimization Control Corridor A", "A", "B", 10.0,
                        CorridorStatus.OPERATIONAL));
        Corridor corridorB = corridorRepository.save(
                new Corridor("COR-OPCTRL-B", "Optimization Control Corridor B", "C", "D", 12.0,
                        CorridorStatus.OPERATIONAL));

        MaintenanceTask task = taskRepository.save(new MaintenanceTask(
                "TASK-OPCTRL-1", "Feasible optimization task", "desc", TaskType.INSPECTION,
                Priority.LOW, 120, corridorA, null, null, D, D.plusDays(1),
                D, D.plusDays(1), TaskStatus.REQUESTED, null));
        taskId = task.getId();

        MaintenanceTask other = taskRepository.save(new MaintenanceTask(
                "TASK-OPCTRL-2", "Second optimization task", "desc", TaskType.INSPECTION,
                Priority.HIGH, 60, corridorB, null, null, D, D,
                D, D, TaskStatus.REQUESTED, null));
        otherTaskId = other.getId();
    }

    @Test
    void runOptimizationReturnsDetailWithScheduledResult() throws Exception {
        String body = """
                {"windowStart":"%s","windowEnd":"%s","taskIds":[%d,%d]}
                """.formatted(D, D.plusDays(1), taskId, otherTaskId);

        mockMvc.perform(post("/api/optimization/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm", is("deterministic-greedy-v1")))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.tasksScheduled", is(2)))
                .andExpect(jsonPath("$.tasksRejected", is(0)))
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[?(@.decision == 'SCHEDULED')]", hasSize(2)))
                .andExpect(jsonPath("$.results[0].blockCode", org.hamcrest.Matchers.startsWith("OPT-")));
    }

    @Test
    void getRunReturnsSummary() throws Exception {
        Long runId = persistRun();

        mockMvc.perform(get("/api/optimization/runs/" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(runId.intValue())))
                .andExpect(jsonPath("$.algorithm", is("deterministic-greedy-v1")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void getResultsReturnsPerTaskOutcomes() throws Exception {
        Long runId = persistRun();

        mockMvc.perform(get("/api/optimization/runs/" + runId + "/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].decision", is("SCHEDULED")))
                .andExpect(jsonPath("$[0].blockCode", org.hamcrest.Matchers.startsWith("OPT-")));
    }

    @Test
    void missingRunReturns404() throws Exception {
        mockMvc.perform(get("/api/optimization/runs/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/optimization/runs/999999/results"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reversedWindowReturns400() throws Exception {
        String body = """
                {"windowStart":"%s","windowEnd":"%s","taskIds":[%d]}
                """.formatted(D.plusDays(3), D, taskId);

        mockMvc.perform(post("/api/optimization/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTaskIdReturns400() throws Exception {
        String body = """
                {"windowStart":"%s","windowEnd":"%s","taskIds":[999999]}
                """.formatted(D, D.plusDays(1));

        mockMvc.perform(post("/api/optimization/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private Long persistRun() throws Exception {
        String response = mockMvc.perform(post("/api/optimization/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"windowStart\":\"" + D + "\",\"windowEnd\":\"" + D.plusDays(1)
                                + "\",\"taskIds\":[" + taskId + "," + otherTaskId + "]}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private Long extractId(String json) {
        // Minimal parse: {"id":123,...}
        int start = json.indexOf("\"id\":") + 5;
        int end = json.indexOf(',', start);
        return Long.parseLong(json.substring(start, end).trim());
    }
}

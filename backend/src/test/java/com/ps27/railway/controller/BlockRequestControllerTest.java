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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BlockRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorridorRepository corridorRepository;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    @Autowired
    private BlockRequestRepository blockRequestRepository;

    private Long corridorId;
    private Long taskId;

    @BeforeEach
    void setUp() {
        blockRequestRepository.deleteAll();
        taskRepository.deleteAll();
        corridorRepository.deleteAll();

        Corridor corridor = corridorRepository.save(
                new Corridor("COR-BLK", "Block Corridor", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
        corridorId = corridor.getId();

        MaintenanceTask task = taskRepository.save(new MaintenanceTask(
                "TASK-BLK", "Block task", "desc", TaskType.INSPECTION, Priority.HIGH, 120,
                corridor, null, null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2),
                null, null, TaskStatus.REQUESTED, null));
        taskId = task.getId();
    }

    @Test
    void createAndReadBlockRequest() throws Exception {
        String body = """
                {
                  "blockCode": "BLK-API-1",
                  "maintenanceTaskId": %d,
                  "corridorId": %d,
                  "requestedStart": "2026-12-01",
                  "requestedEnd": "2026-12-02",
                  "status": "REQUESTED"
                }
                """.formatted(taskId, corridorId);

        mockMvc.perform(post("/api/block-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blockCode").value("BLK-API-1"))
                .andExpect(jsonPath("$.taskCode").value("TASK-BLK"));

        mockMvc.perform(get("/api/block-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].blockCode").value("BLK-API-1"));
    }

    @Test
    void createRejectsReversedDates() throws Exception {
        String body = """
                {
                  "blockCode": "BLK-API-BAD",
                  "maintenanceTaskId": %d,
                  "corridorId": %d,
                  "requestedStart": "2026-12-10",
                  "requestedEnd": "2026-12-01"
                }
                """.formatted(taskId, corridorId);

        mockMvc.perform(post("/api/block-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "requestedEnd must be after or equal to requestedStart"));
    }

    @Test
    void createRejectsUnknownTask() throws Exception {
        String body = """
                {
                  "blockCode": "BLK-API-X",
                  "maintenanceTaskId": 999999,
                  "corridorId": %d,
                  "requestedStart": "2026-12-01",
                  "requestedEnd": "2026-12-02"
                }
                """.formatted(corridorId);

        mockMvc.perform(post("/api/block-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

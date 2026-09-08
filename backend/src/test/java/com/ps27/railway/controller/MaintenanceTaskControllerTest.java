package com.ps27.railway.controller;

import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MaintenanceTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorridorRepository corridorRepository;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    private Long corridorId;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        corridorRepository.deleteAll();
        Corridor corridor = corridorRepository.save(
                new Corridor("COR-API", "Api Corridor", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
        corridorId = corridor.getId();
    }

    @Test
    void createReadUpdateDeleteTask() throws Exception {
        String body = """
                {
                  "taskCode": "API-TASK-1",
                  "title": "API test task",
                  "description": "desc",
                  "taskType": "INSPECTION",
                  "priority": "HIGH",
                  "durationMinutes": 120,
                  "corridorId": %d,
                  "requestedStart": "2026-11-01",
                  "requestedEnd": "2026-11-02",
                  "status": "REQUESTED"
                }
                """.formatted(corridorId);

        String created = mockMvc.perform(post("/api/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskCode").value("API-TASK-1"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn().getResponse().getContentAsString();

        long id = extractId(created);

        mockMvc.perform(get("/api/maintenance/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("API test task"));

        mockMvc.perform(get("/api/maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskCode").value("API-TASK-1"));

        mockMvc.perform(put("/api/maintenance/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("API-TASK-1", "API-TASK-1-UPD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskCode").value("API-TASK-1-UPD"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/maintenance/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/maintenance/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithMissingRequiredFieldReturns400() throws Exception {
        String body = """
                {
                  "title": "No task code",
                  "taskType": "INSPECTION",
                  "priority": "HIGH",
                  "durationMinutes": 120,
                  "corridorId": %d
                }
                """.formatted(corridorId);

        mockMvc.perform(post("/api/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void createWithInvalidStatusReturns400() throws Exception {
        String body = """
                {
                  "taskCode": "API-TASK-BAD",
                  "title": "Bad status",
                  "taskType": "INSPECTION",
                  "priority": "HIGH",
                  "durationMinutes": 120,
                  "corridorId": %d,
                  "status": "NOT_A_STATUS"
                }
                """.formatted(corridorId);

        mockMvc.perform(post("/api/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithInvalidCorridorReferenceReturns400() throws Exception {
        String body = """
                {
                  "taskCode": "API-TASK-X",
                  "title": "Bad corridor",
                  "taskType": "INSPECTION",
                  "priority": "HIGH",
                  "durationMinutes": 120,
                  "corridorId": 999999
                }
                """;

        mockMvc.perform(post("/api/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corridor not found: 999999"));
    }

    @Test
    void createWithReversedDatesReturns400() throws Exception {
        String body = """
                {
                  "taskCode": "API-TASK-DATES",
                  "title": "Reversed dates",
                  "taskType": "INSPECTION",
                  "priority": "HIGH",
                  "durationMinutes": 120,
                  "corridorId": %d,
                  "requestedStart": "2026-11-10",
                  "requestedEnd": "2026-11-01"
                }
                """.formatted(corridorId);

        mockMvc.perform(post("/api/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "requestedEnd must be after or equal to requestedStart"));
    }

    @Test
    void getMissingTaskReturns404() throws Exception {
        mockMvc.perform(get("/api/maintenance/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private long extractId(String json) {
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }
}

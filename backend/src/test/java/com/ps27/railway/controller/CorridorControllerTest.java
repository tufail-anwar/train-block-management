package com.ps27.railway.controller;

import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.entity.Train;
import com.ps27.railway.entity.TrainSchedule;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CorridorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorridorRepository corridorRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TrainScheduleRepository trainScheduleRepository;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    @Autowired
    private BlockRequestRepository blockRequestRepository;

    private Long corridorId;

    @BeforeEach
    void setUp() {
        blockRequestRepository.deleteAll();
        taskRepository.deleteAll();
        trainScheduleRepository.deleteAll();
        corridorRepository.deleteAll();
        trainRepository.deleteAll();

        Corridor corridor = corridorRepository.save(
                new Corridor("COR-AVAIL", "Avail Corridor", "A", "B", 10.0, CorridorStatus.OPERATIONAL));
        corridorId = corridor.getId();

        Train train = trainRepository.save(
                new Train("TRN-AVAIL", "Avail Train", TrainType.PASSENGER, true));
        trainScheduleRepository.save(new TrainSchedule(train, corridor,
                LocalDate.of(2026, 12, 1), null,
                LocalTime.of(6, 0), LocalTime.of(8, 0), Direction.UP));

        MaintenanceTask task = taskRepository.save(new MaintenanceTask(
                "TASK-AVAIL", "Avail task", "desc", TaskType.INSPECTION, Priority.MEDIUM, 120,
                corridor, null, null, LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 4),
                null, null, TaskStatus.REQUESTED, null));
        blockRequestRepository.save(new com.ps27.railway.entity.BlockRequest(
                "BLK-AVAIL", task, corridor, LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 4),
                com.ps27.railway.enums.BlockRequestStatus.REQUESTED, null));
    }

    @Test
    void availabilityListsTrainsAndBlocks() throws Exception {
        mockMvc.perform(get("/api/corridors/{id}/availability", corridorId)
                        .param("from", "2026-11-30")
                        .param("to", "2026-12-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corridorId").value(corridorId))
                .andExpect(jsonPath("$.trains.length()").value(1))
                .andExpect(jsonPath("$.trains[0].trainCode").value("TRN-AVAIL"))
                .andExpect(jsonPath("$.blocks.length()").value(1))
                .andExpect(jsonPath("$.blocks[0].blockCode").value("BLK-AVAIL"));
    }

    @Test
    void availabilityMissingRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/corridors/{id}/availability", corridorId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availabilityReversedRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/corridors/{id}/availability", corridorId)
                        .param("from", "2026-12-05")
                        .param("to", "2026-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'to' must be after or equal to 'from'"));
    }

    @Test
    void availabilityUnknownCorridorReturns404() throws Exception {
        mockMvc.perform(get("/api/corridors/999999/availability")
                        .param("from", "2026-11-30")
                        .param("to", "2026-12-05"))
                .andExpect(status().isNotFound());
    }
}

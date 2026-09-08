package com.ps27.railway.controller;

import com.ps27.railway.entity.Corridor;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.repository.BlockRequestRepository;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CsvImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorridorRepository corridorRepository;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    @Autowired
    private BlockRequestRepository blockRequestRepository;

    @BeforeEach
    void setUp() {
        blockRequestRepository.deleteAll();
        taskRepository.deleteAll();
        corridorRepository.deleteAll();
        corridorRepository.save(new Corridor("COR-IMPORT", "Import Corridor", "A", "B", 10.0,
                CorridorStatus.OPERATIONAL));
    }

    @Test
    void importCsvViaEndpoint() throws Exception {
        String csv = """
                taskCode,title,description,taskType,priority,durationMinutes,corridorCode,departmentCode,requestedStart,requestedEnd,earliestStart,latestEnd,status
                IMP-END-1,Imported task,desc,INSPECTION,HIGH,120,COR-IMPORT,,2026-12-01,2026-12-02,,,REQUESTED
                """;
        MockMultipartFile file = new MockMultipartFile("file", "tasks.csv",
                "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/maintenance/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));
    }

    @Test
    void importNonCsvReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "tasks.txt",
                "text/plain", "not csv".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/maintenance/import").file(file))
                .andExpect(status().isBadRequest());
    }
}

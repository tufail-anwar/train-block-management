package com.ps27.railway.service;

import com.ps27.railway.dto.CsvImportResult;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.Department;
import com.ps27.railway.enums.CorridorStatus;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.DepartmentRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CsvImportServiceTest {

    @Autowired
    private CsvImportService csvImportService;

    @Autowired
    private MaintenanceTaskRepository taskRepository;

    @Autowired
    private CorridorRepository corridorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void importsValidCsvRows() {
        corridorRepository.save(new Corridor("COR-IMP", "Import Corridor", "A", "B", 10.0,
                CorridorStatus.OPERATIONAL));
        departmentRepository.save(new Department("DEPT-IMP", "Import Dept", "desc"));

        String csv = """
                taskCode,title,description,taskType,priority,durationMinutes,corridorCode,departmentCode,requestedStart,requestedEnd,earliestStart,latestEnd,status
                IMP-001,Task one,desc,INSPECTION,HIGH,120,COR-IMP,DEPT-IMP,2026-12-01,2026-12-02,,,REQUESTED
                IMP-002,Task two,desc,REPLACEMENT,CRITICAL,240,COR-IMP,DEPT-IMP,2026-12-05,2026-12-06,,,REQUESTED
                """;
        MockMultipartFile file = new MockMultipartFile("file", "tasks.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = csvImportService.importTasks(file);

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.skipped()).isZero();
        assertThat(result.errors()).isEmpty();
        assertThat(taskRepository.existsByTaskCode("IMP-001")).isTrue();
        assertThat(taskRepository.existsByTaskCode("IMP-002")).isTrue();
    }

    @Test
    void skipsInvalidRowsAndReportsErrors() {
        corridorRepository.save(new Corridor("COR-IMP2", "Import Corridor 2", "A", "B", 10.0,
                CorridorStatus.OPERATIONAL));

        String csv = """
                taskCode,title,description,taskType,priority,durationMinutes,corridorCode,departmentCode,requestedStart,requestedEnd,earliestStart,latestEnd,status
                IMP-BAD,Missing fields,,INSPECTION,,120,COR-IMP2,,2026-12-01,2026-12-02,,,REQUESTED
                IMP-OK,Valid task,,REPLACEMENT,HIGH,240,COR-IMP2,,2026-12-05,2026-12-06,,,REQUESTED
                """;
        MockMultipartFile file = new MockMultipartFile("file", "tasks.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = csvImportService.importTasks(file);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0)).contains("Line 2");
    }
}

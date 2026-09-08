package com.ps27.railway.controller;

import com.ps27.railway.dto.CsvImportResult;
import com.ps27.railway.service.CsvImportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/maintenance")
public class ImportController {

    private final CsvImportService csvImportService;

    public ImportController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    /**
     * Import synthetic maintenance tasks from a CSV file.
     * Expected columns: taskCode,title,description,taskType,priority,durationMinutes,
     * corridorCode,departmentCode,requestedStart,requestedEnd,earliestStart,latestEnd,status
     */
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.OK)
    public CsvImportResult importTasks(@RequestPart("file") MultipartFile file) {
        return csvImportService.importTasks(file);
    }
}

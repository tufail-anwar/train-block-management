package com.ps27.railway.service;

import com.ps27.railway.dto.CsvImportResult;
import com.ps27.railway.entity.Corridor;
import com.ps27.railway.entity.Department;
import com.ps27.railway.entity.MaintenanceTask;
import com.ps27.railway.enums.Priority;
import com.ps27.railway.enums.TaskStatus;
import com.ps27.railway.enums.TaskType;
import com.ps27.railway.exception.BadRequestException;
import com.ps27.railway.repository.CorridorRepository;
import com.ps27.railway.repository.DepartmentRepository;
import com.ps27.railway.repository.MaintenanceTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports synthetic maintenance tasks from a CSV file.
 *
 * Expected CSV header (values may be empty where optional):
 * taskCode,title,description,taskType,priority,durationMinutes,corridorCode,
 * departmentCode,requestedStart,requestedEnd,earliestStart,latestEnd,status
 */
@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    private final MaintenanceTaskRepository taskRepository;
    private final CorridorRepository corridorRepository;
    private final DepartmentRepository departmentRepository;

    public CsvImportService(MaintenanceTaskRepository taskRepository,
                            CorridorRepository corridorRepository,
                            DepartmentRepository departmentRepository) {
        this.taskRepository = taskRepository;
        this.corridorRepository = corridorRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public CsvImportResult importTasks(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV file must not be empty");
        }
        if (!"text/csv".equalsIgnoreCase(file.getContentType())
                && !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("Only CSV files are supported");
        }

        List<String> errors = new ArrayList<>();
        int imported = 0;
        int totalRows = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                throw new BadRequestException("CSV file is empty");
            }

            String line;
            int lineNumber = 1; // header already consumed
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                totalRows++;
                String[] cols = splitCsv(line);
                try {
                    importRow(cols, lineNumber);
                    imported++;
                } catch (RuntimeException ex) {
                    errors.add("Line " + lineNumber + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            log.error("Failed to read CSV file", ex);
            throw new BadRequestException("Failed to read CSV file");
        }

        return new CsvImportResult(totalRows, imported, totalRows - imported, errors);
    }

    private void importRow(String[] cols, int lineNumber) {
        String taskCode = value(cols, 0);
        String title = value(cols, 1);
        if (taskCode == null || title == null) {
            throw new IllegalArgumentException("taskCode and title are required");
        }
        if (taskRepository.existsByTaskCode(taskCode)) {
            throw new IllegalArgumentException("taskCode already exists: " + taskCode);
        }

        String corridorCode = value(cols, 6);
        Corridor corridor = corridorCode != null
                ? corridorRepository.findByCode(corridorCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown corridor code: " + corridorCode))
                : null;
        if (corridor == null) {
            throw new IllegalArgumentException("corridorCode is required and must reference an existing corridor");
        }

        Department department = null;
        String departmentCode = value(cols, 7);
        if (departmentCode != null && !departmentCode.isBlank()) {
            department = departmentRepository.findByCode(departmentCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown department code: " + departmentCode));
        }

        TaskType taskType = parseEnum(TaskType.class, value(cols, 3), "taskType");
        Priority priority = parseEnum(Priority.class, value(cols, 4), "priority");
        TaskStatus status = value(cols, 12) != null && !value(cols, 12).isBlank()
                ? parseEnum(TaskStatus.class, value(cols, 12), "status")
                : TaskStatus.DRAFT;

        int durationMinutes = parseDuration(value(cols, 5));
        LocalDate requestedStart = parseDate(value(cols, 8), "requestedStart");
        LocalDate requestedEnd = parseDate(value(cols, 9), "requestedEnd");
        LocalDate earliestStart = parseDate(value(cols, 10), "earliestStart");
        LocalDate latestEnd = parseDate(value(cols, 11), "latestEnd");

        if (requestedStart != null && requestedEnd != null && requestedEnd.isBefore(requestedStart)) {
            throw new IllegalArgumentException("requestedEnd must be after or equal to requestedStart");
        }
        if (earliestStart != null && latestEnd != null && latestEnd.isBefore(earliestStart)) {
            throw new IllegalArgumentException("latestEnd must be after or equal to earliestStart");
        }

        MaintenanceTask task = new MaintenanceTask(taskCode, title, value(cols, 2), taskType,
                priority, durationMinutes, corridor, null, department,
                requestedStart, requestedEnd, earliestStart, latestEnd, status,
                "Imported from CSV");
        taskRepository.save(task);
    }

    private int parseDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("durationMinutes is required");
        }
        try {
            int minutes = Integer.parseInt(raw.trim());
            if (minutes <= 0) {
                throw new IllegalArgumentException("durationMinutes must be positive");
            }
            return minutes;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("durationMinutes must be an integer: " + raw);
        }
    }

    private LocalDate parseDate(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(field + " must be a valid date (yyyy-MM-dd): " + raw);
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid " + field + ": " + raw);
        }
    }

    private String value(String[] cols, int index) {
        if (cols == null || index >= cols.length) {
            return null;
        }
        String v = cols[index].trim();
        return v.isEmpty() ? null : v;
    }

    /** Minimal CSV splitter that honors simple double-quoted fields. */
    private String[] splitCsv(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values.toArray(new String[0]);
    }
}

package com.ps27.railway.dto;

import java.util.List;

/** Result of a CSV import operation. */
public record CsvImportResult(int totalRows, int imported, int skipped,
                              List<String> errors) {

    public static CsvImportResult empty() {
        return new CsvImportResult(0, 0, 0, List.of());
    }
}

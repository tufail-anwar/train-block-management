package com.ps27.railway.dto;

import com.ps27.railway.enums.ResourceType;

public record MaintenanceResourceResponse(Long id, String code, String name, ResourceType resourceType,
                                          Long departmentId, String departmentName,
                                          int capacityPerShift, boolean active) {
}

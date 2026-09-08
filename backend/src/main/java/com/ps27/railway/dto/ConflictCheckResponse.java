package com.ps27.railway.dto;

import java.util.List;

/**
 * Result of a what-if conflict check for a proposed block request that was
 * validated WITHOUT persisting. `conflicts` is empty when the proposal is clean.
 */
public record ConflictCheckResponse(
        String blockCode,
        List<ConflictResponse> conflicts) {
}

package com.stelli.stelli_backend.voice;

import java.util.Map;

/**
 * The pre-populated set of Field Values returned after Field Extraction, keyed by
 * Field id. Mirrors the entry's {@code fieldValues} shape so the User can review and
 * save it via the existing create-entry endpoint.
 */
public record PartialFillResponse(Map<Long, Object> fieldValues) {}

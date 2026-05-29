package com.stelli.stelli_backend.entry;

import java.time.LocalDateTime;
import java.util.Map;

public record EntryResponse(
    Long id,
    Long listId,
    Map<String, Object> fieldValues,
    String searchText,
    LocalDateTime createdAt
) {}

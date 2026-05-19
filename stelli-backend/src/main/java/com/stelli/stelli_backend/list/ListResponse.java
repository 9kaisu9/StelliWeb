package com.stelli.stelli_backend.list;

import java.time.LocalDateTime;
import java.util.List;

public record ListResponse(
    Long id,
    String name,
    String description,
    String icon,
    List<FieldResponse> fields,
    LocalDateTime createdAt
) {}

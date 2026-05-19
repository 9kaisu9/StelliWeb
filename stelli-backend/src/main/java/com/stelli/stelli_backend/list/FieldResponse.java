package com.stelli.stelli_backend.list;

import java.util.List;

public record FieldResponse(
    Long id,
    String name,
    FieldType type,
    boolean required,
    int displayOrder,
    List<String> choices
) {}

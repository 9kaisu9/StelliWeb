package com.stelli.stelli_backend.list;

import java.util.List;

public record FieldRequest(
    String name,
    FieldType type,
    boolean required,
    int displayOrder,
    List<String> choices
) {
    public FieldRequest {
        choices = choices != null ? choices : List.of();
    }
}

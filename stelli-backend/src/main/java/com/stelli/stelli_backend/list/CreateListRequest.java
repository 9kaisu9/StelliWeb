package com.stelli.stelli_backend.list;

import java.util.List;

public record CreateListRequest(
    String name,
    String description,
    String icon,
    List<FieldRequest> fields
) {}

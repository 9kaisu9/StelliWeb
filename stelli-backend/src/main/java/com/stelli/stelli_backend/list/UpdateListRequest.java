package com.stelli.stelli_backend.list;

public record UpdateListRequest(
    String name,
    String description,
    String icon
) {}

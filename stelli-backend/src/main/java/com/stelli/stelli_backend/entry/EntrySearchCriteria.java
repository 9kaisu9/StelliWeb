package com.stelli.stelli_backend.entry;

public record EntrySearchCriteria(
    String sortField,
    String sortDir,
    String filterField,
    String filterValue,
    String q
) {}

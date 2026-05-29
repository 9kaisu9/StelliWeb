package com.stelli.stelli_backend.entry;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists/{listId}/entries")
@RequiredArgsConstructor
public class EntryController {

    private final EntryService entryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryResponse create(@PathVariable Long listId, @RequestBody CreateEntryRequest request) {
        return entryService.create(listId, request);
    }

    @GetMapping
    public List<EntryResponse> getAll(@PathVariable Long listId) {
        return entryService.findAllByListId(listId);
    }

    @GetMapping("/{id}")
    public EntryResponse getById(@PathVariable Long listId, @PathVariable Long id) {
        return entryService.findById(listId, id);
    }
}

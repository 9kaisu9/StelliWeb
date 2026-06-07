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
    public List<EntryResponse> getAll(@PathVariable Long listId,
                                      @RequestParam(required = false) String sortField,
                                      @RequestParam(required = false) String sortDir,
                                      @RequestParam(required = false) String filterField,
                                      @RequestParam(required = false) String filterValue,
                                      @RequestParam(required = false) String q) {
        return entryService.search(listId,
            new EntrySearchCriteria(sortField, sortDir, filterField, filterValue, q));
    }

    @GetMapping("/{id}")
    public EntryResponse getById(@PathVariable Long listId, @PathVariable Long id) {
        return entryService.findById(listId, id);
    }

    @PutMapping("/{id}")
    public EntryResponse update(@PathVariable Long listId, @PathVariable Long id,
                                @RequestBody UpdateEntryRequest request) {
        return entryService.update(listId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long listId, @PathVariable Long id) {
        entryService.delete(listId, id);
    }
}

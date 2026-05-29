package com.stelli.stelli_backend.list;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
public class ListController {

    private final ListService listService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListResponse create(@RequestBody CreateListRequest request) {
        return listService.create(request);
    }

    @GetMapping
    public List<ListResponse> getAll() {
        return listService.findAll();
    }

    @GetMapping("/{id}")
    public ListResponse getById(@PathVariable Long id) {
        return listService.findById(id);
    }

    @PutMapping("/{id}")
    public ListResponse update(@PathVariable Long id, @RequestBody UpdateListRequest request) {
        return listService.update(id, request);
    }

    @PutMapping("/{id}/fields")
    public ListResponse replaceFields(@PathVariable Long id, @RequestBody java.util.List<FieldRequest> fields) {
        return listService.replaceFields(id, fields);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        listService.delete(id);
    }
}

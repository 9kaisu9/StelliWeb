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
}

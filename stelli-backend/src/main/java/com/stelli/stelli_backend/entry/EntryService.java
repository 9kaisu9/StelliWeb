package com.stelli.stelli_backend.entry;

import com.stelli.stelli_backend.list.FieldRepository;
import com.stelli.stelli_backend.list.FieldType;
import com.stelli.stelli_backend.list.ListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EntryService {

    private static final Set<FieldType> SEARCHABLE = Set.of(
        FieldType.TEXT, FieldType.NUMBER, FieldType.RATING,
        FieldType.OPTION, FieldType.MULTI_OPTION, FieldType.DATE
    );

    private final EntryRepository entryRepository;
    private final ListRepository listRepository;
    private final FieldRepository fieldRepository;

    public EntryResponse create(Long listId, CreateEntryRequest request) {
        if (!listRepository.existsById(listId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var fields = fieldRepository.findByListId(listId);
        var searchText = buildSearchText(fields, request.fieldValues());

        var entry = new Entry();
        entry.setListId(listId);
        entry.setFieldValues(request.fieldValues());
        entry.setSearchText(searchText);
        return toResponse(entryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<EntryResponse> search(Long listId, EntrySearchCriteria criteria) {
        if (!listRepository.existsById(listId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return entryRepository.search(listId, criteria).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EntryResponse findById(Long listId, Long entryId) {
        if (!listRepository.existsById(listId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return entryRepository.findById(entryId)
            .filter(e -> e.getListId().equals(listId))
            .map(this::toResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public EntryResponse update(Long listId, Long entryId, UpdateEntryRequest request) {
        var entry = entryRepository.findById(entryId)
            .filter(e -> e.getListId().equals(listId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var fields = fieldRepository.findByListId(listId);
        entry.setFieldValues(request.fieldValues());
        entry.setSearchText(buildSearchText(fields, request.fieldValues()));
        return toResponse(entryRepository.save(entry));
    }

    public void delete(Long listId, Long entryId) {
        var entry = entryRepository.findById(entryId)
            .filter(e -> e.getListId().equals(listId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        entryRepository.delete(entry);
    }

    private String buildSearchText(List<com.stelli.stelli_backend.list.Field> fields, Map<String, Object> fieldValues) {
        return fields.stream()
            .filter(f -> SEARCHABLE.contains(f.getType()))
            .map(f -> fieldValues.get(String.valueOf(f.getId())))
            .filter(v -> v != null)
            .map(v -> v instanceof Collection<?> col
                ? col.stream().map(Object::toString).collect(Collectors.joining(" "))
                : v.toString())
            .collect(Collectors.joining(" "));
    }

    private EntryResponse toResponse(Entry entry) {
        return new EntryResponse(entry.getId(), entry.getListId(),
            entry.getFieldValues(), entry.getSearchText(), entry.getCreatedAt());
    }
}

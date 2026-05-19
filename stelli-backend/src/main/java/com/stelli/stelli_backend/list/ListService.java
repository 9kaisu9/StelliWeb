package com.stelli.stelli_backend.list;

import com.stelli.stelli_backend.user.DataSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ListService {

    private final ListRepository listRepository;

    public ListResponse create(CreateListRequest request) {
        StelliList list = new StelliList();
        list.setName(request.name());
        list.setDescription(request.description());
        list.setIcon(request.icon());
        list.setUserId(DataSeeder.DEFAULT_USER_ID);

        for (FieldRequest fr : request.fields()) {
            if ((fr.type() == FieldType.OPTION || fr.type() == FieldType.MULTI_OPTION)
                    && fr.choices().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        fr.type() + " fields require at least one choice");
            }
            Field field = new Field();
            field.setName(fr.name());
            field.setType(fr.type());
            field.setRequired(fr.required());
            field.setDisplayOrder(fr.displayOrder());
            field.setChoices(new ArrayList<>(fr.choices()));
            field.setList(list);
            list.getFields().add(field);
        }

        return toResponse(listRepository.save(list));
    }

    @Transactional(readOnly = true)
    public List<ListResponse> findAll() {
        return listRepository.findByUserId(DataSeeder.DEFAULT_USER_ID)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ListResponse findById(Long id) {
        return listRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ListResponse toResponse(StelliList list) {
        List<FieldResponse> fields = list.getFields().stream()
                .map(f -> new FieldResponse(f.getId(), f.getName(), f.getType(),
                        f.isRequired(), f.getDisplayOrder(), f.getChoices()))
                .toList();
        return new ListResponse(list.getId(), list.getName(), list.getDescription(),
                list.getIcon(), fields, list.getCreatedAt());
    }
}

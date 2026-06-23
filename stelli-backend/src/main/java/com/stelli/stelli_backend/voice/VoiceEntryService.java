package com.stelli.stelli_backend.voice;

import com.stelli.stelli_backend.list.Field;
import com.stelli.stelli_backend.list.FieldRepository;
import com.stelli.stelli_backend.list.FieldType;
import com.stelli.stelli_backend.list.ListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceEntryService {

    // Field types that cannot be derived from speech — never present in a Partial Fill.
    private static final Set<FieldType> NON_EXTRACTABLE =
        Set.of(FieldType.IMAGE, FieldType.VIDEO, FieldType.LOCATION);

    private final ListRepository listRepository;
    private final FieldRepository fieldRepository;
    private final VoiceTranscriber transcriber;
    private final FieldExtractor fieldExtractor;

    public PartialFillResponse createPartialFill(Long listId, MultipartFile audio) {
        if (!listRepository.existsById(listId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        var transcript = transcriber.transcribe(toTempFile(audio));
        var fields = fieldRepository.findByListId(listId);
        var extractable = fields.stream()
            .filter(f -> !NON_EXTRACTABLE.contains(f.getType()))
            .toList();

        var raw = fieldExtractor.extract(transcript, extractable);

        // Defensively drop anything the extractor returned for a non-extractable Field.
        var typesById = fields.stream()
            .collect(Collectors.toMap(Field::getId, Field::getType));
        var partialFill = raw.entrySet().stream()
            .filter(e -> !NON_EXTRACTABLE.contains(typesById.get(e.getKey())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (a, b) -> b, LinkedHashMap::new));

        return new PartialFillResponse(partialFill);
    }

    private Path toTempFile(MultipartFile audio) {
        try {
            var tmp = Files.createTempFile("voice-memo-", suffixOf(audio));
            audio.transferTo(tmp);
            tmp.toFile().deleteOnExit();
            return tmp;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to buffer voice memo", e);
        }
    }

    private String suffixOf(MultipartFile audio) {
        var name = audio.getOriginalFilename();
        var dot = name == null ? -1 : name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }
}

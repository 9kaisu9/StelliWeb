package com.stelli.stelli_backend.voice;

import com.stelli.stelli_backend.list.Field;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Real {@link FieldExtractor} backed by Ollama via Spring AI. Sends the transcript and
 * the List's Field schema to the LLM and parses a JSON object of {@code fieldId -> value}.
 * The model is configurable via {@code OLLAMA_MODEL}.
 */
@Service
public class OllamaFieldExtractor implements FieldExtractor {

    private static final String SYSTEM_PROMPT = """
        You extract structured data from a transcript of someone describing a list entry.
        You are given the list's fields. Return ONLY a JSON object mapping field id (as a
        string) to the value mentioned in the transcript. Include a field ONLY if it is
        clearly mentioned — never guess. Omit fields that are not mentioned. For OPTION and
        MULTI_OPTION fields, choose only from the provided choices. Return {} if nothing is
        mentioned.
        """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public OllamaFieldExtractor(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<Long, Object> extract(String transcript, List<Field> schema) {
        var userPrompt = "Fields:\n" + describe(schema) + "\n\nTranscript:\n" + transcript;

        var content = chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(userPrompt)
            .call()
            .content();

        return parse(content);
    }

    private String describe(List<Field> schema) {
        return schema.stream()
            .map(f -> "- id=" + f.getId() + ", name=\"" + f.getName() + "\", type=" + f.getType()
                + (f.getChoices().isEmpty() ? "" : ", choices=" + f.getChoices()))
            .collect(Collectors.joining("\n"));
    }

    private Map<Long, Object> parse(String content) {
        if (content == null) {
            return Map.of();
        }
        var json = content.substring(content.indexOf('{'), content.lastIndexOf('}') + 1);
        Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<>() {});
        var result = new LinkedHashMap<Long, Object>();
        raw.forEach((key, value) -> {
            try {
                result.put(Long.parseLong(key.trim()), value);
            } catch (NumberFormatException ignored) {
                // skip keys the model did not return as a numeric field id
            }
        });
        return result;
    }
}

package com.stelli.stelli_backend.voice;

import com.stelli.stelli_backend.list.Field;

import java.util.List;
import java.util.Map;

/**
 * System boundary over the LLM (Ollama). Maps a transcript onto a List's Field
 * schema, returning only the Fields it could confidently identify (a Partial Fill),
 * keyed by Field id.
 */
public interface FieldExtractor {
    Map<Long, Object> extract(String transcript, List<Field> schema);
}

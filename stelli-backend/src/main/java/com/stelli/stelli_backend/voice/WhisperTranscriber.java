package com.stelli.stelli_backend.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Real {@link VoiceTranscriber} that shells out to the Whisper CLI and parses its
 * JSON output. The model size is configurable via {@code WHISPER_MODEL}.
 */
@Service
public class WhisperTranscriber implements VoiceTranscriber {

    private final String model;
    private final ObjectMapper objectMapper;

    public WhisperTranscriber(@Value("${app.whisper.model}") String model, ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
    }

    @Override
    public String transcribe(Path audioFile) {
        try {
            var outputDir = Files.createTempDirectory("whisper-");
            var process = new ProcessBuilder(
                "whisper", audioFile.toString(),
                "--model", model,
                "--output_format", "json",
                "--output_dir", outputDir.toString())
                .redirectErrorStream(true)
                .start();

            var log = new String(process.getInputStream().readAllBytes());
            if (process.waitFor() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Whisper transcription failed: " + log);
            }

            var jsonFile = outputDir.resolve(baseName(audioFile) + ".json");
            var node = objectMapper.readTree(Files.readString(jsonFile));
            return node.get("text").asText().trim();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Whisper transcription failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Whisper transcription was interrupted", e);
        }
    }

    private String baseName(Path file) {
        var name = file.getFileName().toString();
        var dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }
}

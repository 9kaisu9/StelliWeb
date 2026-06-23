package com.stelli.stelli_backend.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.uploads-path}") String uploadsPath) {
        this.root = Path.of(uploadsPath).toAbsolutePath().normalize();
    }

    /**
     * Stores the file under {root}/{listId}/{entryId}/{fieldId}/{filename} and
     * returns the path relative to the uploads root, to be saved as the Field Value.
     */
    public String store(Long listId, Long entryId, Long fieldId, MultipartFile file) {
        var relative = Path.of(String.valueOf(listId), String.valueOf(entryId),
            String.valueOf(fieldId), file.getOriginalFilename());
        var target = root.resolve(relative).normalize();
        try {
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file " + relative, e);
        }
        return relative.toString();
    }

    /**
     * Loads a previously stored file by its uploads-root-relative path. Rejects any
     * path that resolves outside the uploads root (path traversal) or does not exist.
     */
    public Resource load(String relativePath) {
        var target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new FileSystemResource(target);
    }
}

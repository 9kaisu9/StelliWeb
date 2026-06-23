package com.stelli.stelli_backend.file;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public FileUploadResponse upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam Long listId,
                                     @RequestParam Long entryId,
                                     @RequestParam Long fieldId) {
        return new FileUploadResponse(fileStorageService.store(listId, entryId, fieldId, file));
    }

    @GetMapping("/{*relativePath}")
    public ResponseEntity<Resource> serve(@PathVariable String relativePath) {
        var resource = fileStorageService.load(relativePath.replaceFirst("^/", ""));
        var contentType = MediaTypeFactory.getMediaType(resource)
            .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(contentType).body(resource);
    }
}

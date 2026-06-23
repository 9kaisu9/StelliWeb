package com.stelli.stelli_backend.voice;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/lists/{listId}/voice-entry")
@RequiredArgsConstructor
public class VoiceEntryController {

    private final VoiceEntryService voiceEntryService;

    @PostMapping
    public PartialFillResponse voiceEntry(@PathVariable Long listId,
                                          @RequestParam("file") MultipartFile file) {
        return voiceEntryService.createPartialFill(listId, file);
    }
}

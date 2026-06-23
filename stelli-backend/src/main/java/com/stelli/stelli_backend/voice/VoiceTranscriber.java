package com.stelli.stelli_backend.voice;

import java.nio.file.Path;

/**
 * System boundary over the Whisper CLI. Transcribes an audio file into text.
 */
public interface VoiceTranscriber {
    String transcribe(Path audioFile);
}

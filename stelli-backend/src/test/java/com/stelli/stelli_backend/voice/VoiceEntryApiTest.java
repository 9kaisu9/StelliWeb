package com.stelli.stelli_backend.voice;

import com.stelli.stelli_backend.list.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class VoiceEntryApiTest {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    tools.jackson.databind.ObjectMapper objectMapper;

    // The two external system boundaries — faked so no real Whisper/Ollama is invoked.
    @MockitoBean
    VoiceTranscriber transcriber;

    @MockitoBean
    FieldExtractor fieldExtractor;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void voiceEntry_allFieldsMentioned_returnsFullPartialFill() throws Exception {
        var listId = createList();
        var fields = getFields(listId);
        var nameId = fields.get(0).get("id").asLong();
        var ratingId = fields.get(1).get("id").asLong();

        when(transcriber.transcribe(any(Path.class))).thenReturn("Sakura Sushi was amazing, five stars");
        when(fieldExtractor.extract(any(String.class), anyList()))
            .thenReturn(Map.of(nameId, "Sakura Sushi", ratingId, 5));

        var audio = new MockMultipartFile("file", "memo.wav", "audio/wav", "fake-audio".getBytes());
        mockMvc.perform(multipart("/api/lists/{listId}/voice-entry", listId).file(audio))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fieldValues." + nameId).value("Sakura Sushi"))
            .andExpect(jsonPath("$.fieldValues." + ratingId).value(5));
    }

    @Test
    void voiceEntry_partialMention_returnsOnlyMentionedFields() throws Exception {
        var listId = createList();
        var fields = getFields(listId);
        var nameId = fields.get(0).get("id").asLong();
        var ratingId = fields.get(1).get("id").asLong();

        when(transcriber.transcribe(any(Path.class))).thenReturn("five stars");
        // Only the rating was mentioned — name is absent from the extraction.
        when(fieldExtractor.extract(any(String.class), anyList()))
            .thenReturn(Map.of(ratingId, 5));

        var audio = new MockMultipartFile("file", "memo.wav", "audio/wav", "fake-audio".getBytes());
        mockMvc.perform(multipart("/api/lists/{listId}/voice-entry", listId).file(audio))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fieldValues." + ratingId).value(5))
            .andExpect(jsonPath("$.fieldValues." + nameId).doesNotExist());
    }

    @Test
    void voiceEntry_neverIncludesImageVideoOrLocationFields() throws Exception {
        var listId = createListWithMediaFields();
        var fields = getFields(listId);
        var titleId = fields.get(0).get("id").asLong();   // TEXT
        var posterId = fields.get(1).get("id").asLong();  // IMAGE
        var placeId = fields.get(2).get("id").asLong();   // LOCATION

        when(transcriber.transcribe(any(Path.class))).thenReturn("transcript");
        // Even if the model erroneously returns values for media/location fields...
        var unfiltered = new java.util.HashMap<Long, Object>();
        unfiltered.put(titleId, "Inception");
        unfiltered.put(posterId, "some/path.png");
        unfiltered.put(placeId, Map.of("lat", 1, "lng", 2));
        when(fieldExtractor.extract(any(String.class), anyList())).thenReturn(unfiltered);

        var audio = new MockMultipartFile("file", "memo.wav", "audio/wav", "fake-audio".getBytes());
        mockMvc.perform(multipart("/api/lists/{listId}/voice-entry", listId).file(audio))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fieldValues." + titleId).value("Inception"))
            .andExpect(jsonPath("$.fieldValues." + posterId).doesNotExist())
            .andExpect(jsonPath("$.fieldValues." + placeId).doesNotExist());
    }

    @Test
    void voiceEntry_listNotFound_returns404() throws Exception {
        var audio = new MockMultipartFile("file", "memo.wav", "audio/wav", "fake-audio".getBytes());

        mockMvc.perform(multipart("/api/lists/{listId}/voice-entry", 999999L).file(audio))
            .andExpect(status().isNotFound());
    }

    @Test
    void voiceEntry_transcriptionFailure_returns500() throws Exception {
        var listId = createList();
        when(transcriber.transcribe(any(Path.class)))
            .thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Whisper transcription failed"));

        var audio = new MockMultipartFile("file", "memo.wav", "audio/wav", "fake-audio".getBytes());
        mockMvc.perform(multipart("/api/lists/{listId}/voice-entry", listId).file(audio))
            .andExpect(status().isInternalServerError());
    }

    private long createList() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
            "name", "Restaurants",
            "description", "Places to visit",
            "icon", "🍽",
            "fields", List.of(
                Map.of("name", "Name", "type", "TEXT", "required", true, "displayOrder", 0, "choices", List.of()),
                Map.of("name", "Rating", "type", "RATING", "required", false, "displayOrder", 1, "choices", List.of())
            )
        ));
        var result = mockMvc.perform(post("/api/lists")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createListWithMediaFields() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
            "name", "Movies",
            "description", "Films",
            "icon", "🎬",
            "fields", List.of(
                Map.of("name", "Title", "type", "TEXT", "required", true, "displayOrder", 0, "choices", List.of()),
                Map.of("name", "Poster", "type", "IMAGE", "required", false, "displayOrder", 1, "choices", List.of()),
                Map.of("name", "Cinema", "type", "LOCATION", "required", false, "displayOrder", 2, "choices", List.of())
            )
        ));
        var result = mockMvc.perform(post("/api/lists")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private tools.jackson.databind.JsonNode getFields(long listId) throws Exception {
        return objectMapper.readTree(
            mockMvc.perform(get("/api/lists/{id}", listId)).andReturn().getResponse().getContentAsString()
        ).get("fields");
    }
}

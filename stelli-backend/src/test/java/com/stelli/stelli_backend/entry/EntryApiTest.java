package com.stelli.stelli_backend.entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class EntryApiTest {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private long createList() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
            "name", "Restaurants",
            "description", "Places I want to visit",
            "icon", "🍽",
            "fields", List.of(
                Map.of("name", "Name", "type", "TEXT", "required", true, "displayOrder", 0, "choices", List.of()),
                Map.of("name", "Rating", "type", "RATING", "required", false, "displayOrder", 1, "choices", List.of()),
                Map.of("name", "Visited", "type", "BOOLEAN", "required", false, "displayOrder", 2, "choices", List.of())
            )
        ));
        var result = mockMvc.perform(post("/api/lists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long getFirstFieldId(long listId) throws Exception {
        var result = mockMvc.perform(get("/api/lists/{id}", listId)).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("fields").get(0).get("id").asLong();
    }

    @Test
    void createEntry_searchTextPopulatedFromSearchableFields() throws Exception {
        var listId = createList();
        var fields = objectMapper.readTree(
            mockMvc.perform(get("/api/lists/{id}", listId)).andReturn().getResponse().getContentAsString()
        ).get("fields");
        var nameFieldId = fields.get(0).get("id").asText();   // TEXT
        var ratingFieldId = fields.get(1).get("id").asText(); // RATING
        var visitedFieldId = fields.get(2).get("id").asText(); // BOOLEAN — not searchable

        var body = objectMapper.writeValueAsString(Map.of(
            "fieldValues", Map.of(
                nameFieldId, "Sakura Sushi",
                ratingFieldId, 5,
                visitedFieldId, true
            )
        ));

        mockMvc.perform(post("/api/lists/{listId}/entries", listId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.searchText").value(org.hamcrest.Matchers.containsString("Sakura Sushi")))
            .andExpect(jsonPath("$.searchText").value(org.hamcrest.Matchers.containsString("5")))
            .andExpect(jsonPath("$.searchText").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("true"))));
    }

    @Test
    void getAllEntries_returnsAllEntriesForList() throws Exception {
        var listId = createList();
        var nameFieldId = getFirstFieldId(listId);

        var body = objectMapper.writeValueAsString(Map.of(
            "fieldValues", Map.of(String.valueOf(nameFieldId), "Sakura Sushi")
        ));
        var created = objectMapper.readTree(
            mockMvc.perform(post("/api/lists/{listId}/entries", listId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andReturn().getResponse().getContentAsString());
        var entryId = created.get("id").asLong();

        mockMvc.perform(get("/api/lists/{listId}/entries", listId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + entryId + ")]").isNotEmpty());
    }

    @Test
    void getEntryById_returnsEntry() throws Exception {
        var listId = createList();
        var nameFieldId = getFirstFieldId(listId);

        var body = objectMapper.writeValueAsString(Map.of(
            "fieldValues", Map.of(String.valueOf(nameFieldId), "Sakura Sushi")
        ));
        var created = objectMapper.readTree(
            mockMvc.perform(post("/api/lists/{listId}/entries", listId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andReturn().getResponse().getContentAsString());
        var entryId = created.get("id").asLong();

        mockMvc.perform(get("/api/lists/{listId}/entries/{id}", listId, entryId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(entryId))
            .andExpect(jsonPath("$.fieldValues." + nameFieldId).value("Sakura Sushi"));
    }

    @Test
    void createEntry_unknownList_returns404() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("fieldValues", Map.of()));

        mockMvc.perform(post("/api/lists/99999/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    void getEntryById_unknownEntry_returns404() throws Exception {
        var listId = createList();

        mockMvc.perform(get("/api/lists/{listId}/entries/99999", listId))
            .andExpect(status().isNotFound());
    }

    @Test
    void createEntry_returns201WithIdAndFieldValues() throws Exception {
        var listId = createList();
        var nameFieldId = getFirstFieldId(listId);

        var body = objectMapper.writeValueAsString(Map.of(
            "fieldValues", Map.of(String.valueOf(nameFieldId), "Sakura Sushi")
        ));

        mockMvc.perform(post("/api/lists/{listId}/entries", listId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.fieldValues." + nameFieldId).value("Sakura Sushi"));
    }
}

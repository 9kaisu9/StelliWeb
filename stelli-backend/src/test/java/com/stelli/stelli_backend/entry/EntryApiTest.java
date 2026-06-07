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

    private long createEntry(long listId, Map<String, Object> fieldValues) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("fieldValues", fieldValues));
        var created = objectMapper.readTree(
            mockMvc.perform(post("/api/lists/{listId}/entries", listId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andReturn().getResponse().getContentAsString());
        return created.get("id").asLong();
    }

    private tools.jackson.databind.JsonNode getFields(long listId) throws Exception {
        return objectMapper.readTree(
            mockMvc.perform(get("/api/lists/{id}", listId)).andReturn().getResponse().getContentAsString()
        ).get("fields");
    }

    @Test
    void filterEntries_byExactFieldValue_returnsOnlyMatching() throws Exception {
        var listId = createList();
        var fields = getFields(listId);
        var nameFieldId = fields.get(0).get("id").asText();
        var ratingFieldId = fields.get(1).get("id").asText();
        createEntry(listId, Map.of(nameFieldId, "Sakura Sushi", ratingFieldId, 5));
        createEntry(listId, Map.of(nameFieldId, "Ramen House", ratingFieldId, 3));

        mockMvc.perform(get("/api/lists/{listId}/entries", listId)
                .param("filterField", ratingFieldId).param("filterValue", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].fieldValues." + nameFieldId).value("Sakura Sushi"));
    }

    @Test
    void sortEntries_byNumericField_ascending() throws Exception {
        var listId = createList();
        var fields = getFields(listId);
        var nameFieldId = fields.get(0).get("id").asText();
        var ratingFieldId = fields.get(1).get("id").asText();
        createEntry(listId, Map.of(nameFieldId, "B", ratingFieldId, 10));
        createEntry(listId, Map.of(nameFieldId, "A", ratingFieldId, 2));
        createEntry(listId, Map.of(nameFieldId, "C", ratingFieldId, 9));

        mockMvc.perform(get("/api/lists/{listId}/entries", listId)
                .param("sortField", ratingFieldId).param("sortDir", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fieldValues." + nameFieldId).value("A"))
            .andExpect(jsonPath("$[1].fieldValues." + nameFieldId).value("C"))
            .andExpect(jsonPath("$[2].fieldValues." + nameFieldId).value("B"));
    }

    @Test
    void sortEntries_byNumericField_descending() throws Exception {
        var listId = createList();
        var fields = getFields(listId);
        var nameFieldId = fields.get(0).get("id").asText();
        var ratingFieldId = fields.get(1).get("id").asText();
        createEntry(listId, Map.of(nameFieldId, "B", ratingFieldId, 10));
        createEntry(listId, Map.of(nameFieldId, "A", ratingFieldId, 2));
        createEntry(listId, Map.of(nameFieldId, "C", ratingFieldId, 9));

        mockMvc.perform(get("/api/lists/{listId}/entries", listId)
                .param("sortField", ratingFieldId).param("sortDir", "desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fieldValues." + nameFieldId).value("B"))
            .andExpect(jsonPath("$[1].fieldValues." + nameFieldId).value("C"))
            .andExpect(jsonPath("$[2].fieldValues." + nameFieldId).value("A"));
    }

    @Test
    void sortEntries_byTextField_ascending() throws Exception {
        var listId = createList();
        var nameFieldId = String.valueOf(getFirstFieldId(listId));
        createEntry(listId, Map.of(nameFieldId, "Charlie"));
        createEntry(listId, Map.of(nameFieldId, "Alpha"));
        createEntry(listId, Map.of(nameFieldId, "Bravo"));

        mockMvc.perform(get("/api/lists/{listId}/entries", listId)
                .param("sortField", nameFieldId).param("sortDir", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fieldValues." + nameFieldId).value("Alpha"))
            .andExpect(jsonPath("$[1].fieldValues." + nameFieldId).value("Bravo"))
            .andExpect(jsonPath("$[2].fieldValues." + nameFieldId).value("Charlie"));
    }

    @Test
    void filterAndSort_combined() throws Exception {
        var listId = createList();
        var fields = getFields(listId);
        var nameFieldId = fields.get(0).get("id").asText();
        var ratingFieldId = fields.get(1).get("id").asText();
        createEntry(listId, Map.of(nameFieldId, "Bravo", ratingFieldId, 5));
        createEntry(listId, Map.of(nameFieldId, "Alpha", ratingFieldId, 5));
        createEntry(listId, Map.of(nameFieldId, "Charlie", ratingFieldId, 2));

        mockMvc.perform(get("/api/lists/{listId}/entries", listId)
                .param("filterField", ratingFieldId).param("filterValue", "5")
                .param("sortField", nameFieldId).param("sortDir", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].fieldValues." + nameFieldId).value("Alpha"))
            .andExpect(jsonPath("$[1].fieldValues." + nameFieldId).value("Bravo"));
    }

    @Test
    void searchEntries_byKeyword_returnsOnlyMatching_caseInsensitive() throws Exception {
        var listId = createList();
        var nameFieldId = String.valueOf(getFirstFieldId(listId));
        createEntry(listId, Map.of(nameFieldId, "Sakura Sushi"));
        createEntry(listId, Map.of(nameFieldId, "Ramen House"));

        mockMvc.perform(get("/api/lists/{listId}/entries", listId).param("q", "sushi"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].fieldValues." + nameFieldId).value("Sakura Sushi"));
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

    @Test
    void updateEntry_updatesFieldValues() throws Exception {
        var listId = createList();
        var nameFieldId = getFirstFieldId(listId);

        var createBody = objectMapper.writeValueAsString(Map.of(
            "fieldValues", Map.of(String.valueOf(nameFieldId), "Sakura Sushi")
        ));
        var entryId = objectMapper.readTree(
            mockMvc.perform(post("/api/lists/{listId}/entries", listId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody))
                .andReturn().getResponse().getContentAsString()
        ).get("id").asLong();

        var updateBody = objectMapper.writeValueAsString(Map.of(
            "fieldValues", Map.of(String.valueOf(nameFieldId), "Ramen House")
        ));
        mockMvc.perform(put("/api/lists/{listId}/entries/{id}", listId, entryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fieldValues." + nameFieldId).value("Ramen House"));
    }

    @Test
    void updateEntry_refreshesSearchText() throws Exception {
        var listId = createList();
        var nameFieldId = getFirstFieldId(listId);

        var createBody = objectMapper.writeValueAsString(Map.of(
            "fieldValues", Map.of(String.valueOf(nameFieldId), "Old Name")
        ));
        var entryId = objectMapper.readTree(
            mockMvc.perform(post("/api/lists/{listId}/entries", listId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody))
                .andReturn().getResponse().getContentAsString()
        ).get("id").asLong();

        var updateBody = objectMapper.writeValueAsString(Map.of(
            "fieldValues", Map.of(String.valueOf(nameFieldId), "New Name")
        ));
        mockMvc.perform(put("/api/lists/{listId}/entries/{id}", listId, entryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.searchText").value(org.hamcrest.Matchers.containsString("New Name")))
            .andExpect(jsonPath("$.searchText").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Old Name"))));
    }

    @Test
    void updateEntry_unknownEntry_returns404() throws Exception {
        var listId = createList();
        var updateBody = objectMapper.writeValueAsString(Map.of("fieldValues", Map.of()));

        mockMvc.perform(put("/api/lists/{listId}/entries/99999", listId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteEntry_removesEntry() throws Exception {
        var listId = createList();
        var nameFieldId = getFirstFieldId(listId);

        var body = objectMapper.writeValueAsString(Map.of(
            "fieldValues", Map.of(String.valueOf(nameFieldId), "Sakura Sushi")
        ));
        var entryId = objectMapper.readTree(
            mockMvc.perform(post("/api/lists/{listId}/entries", listId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andReturn().getResponse().getContentAsString()
        ).get("id").asLong();

        mockMvc.perform(delete("/api/lists/{listId}/entries/{id}", listId, entryId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lists/{listId}/entries/{id}", listId, entryId))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteEntry_unknownEntry_returns404() throws Exception {
        var listId = createList();

        mockMvc.perform(delete("/api/lists/{listId}/entries/99999", listId))
            .andExpect(status().isNotFound());
    }
}

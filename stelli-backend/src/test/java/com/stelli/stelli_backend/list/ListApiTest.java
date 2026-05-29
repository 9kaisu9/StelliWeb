package com.stelli.stelli_backend.list;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class ListApiTest {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private String restaurantListJson() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "name", "Restaurants",
            "description", "Places I want to visit",
            "icon", "🍽",
            "fields", List.of(Map.of(
                "name", "Name", "type", "TEXT", "required", true, "displayOrder", 0, "choices", List.of()
            ))
        ));
    }

    @Test
    void createList_withTextField_returns201WithList() throws Exception {
        mockMvc.perform(post("/api/lists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(restaurantListJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Restaurants"))
            .andExpect(jsonPath("$.fields[0].name").value("Name"))
            .andExpect(jsonPath("$.fields[0].type").value("TEXT"));
    }

    @Test
    void getAllLists_returnsCreatedList() throws Exception {
        mockMvc.perform(post("/api/lists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(restaurantListJson()))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/lists"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Restaurants"));
    }

    @Test
    void getListById_returnsListWithFields() throws Exception {
        var result = mockMvc.perform(post("/api/lists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(restaurantListJson()))
            .andExpect(status().isCreated())
            .andReturn();

        var id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/lists/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Restaurants"))
            .andExpect(jsonPath("$.fields[0].name").value("Name"));
    }

    @Test
    void getListById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/lists/99999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateList_name_returns200WithUpdatedName() throws Exception {
        var created = objectMapper.readTree(
            mockMvc.perform(post("/api/lists")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(restaurantListJson()))
                .andReturn().getResponse().getContentAsString());
        var id = created.get("id").asLong();

        var update = objectMapper.writeValueAsString(Map.of(
            "name", "Best Restaurants", "description", "Updated", "icon", "🍕"
        ));

        mockMvc.perform(put("/api/lists/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(update))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Best Restaurants"))
            .andExpect(jsonPath("$.description").value("Updated"))
            .andExpect(jsonPath("$.icon").value("🍕"));
    }

    @Test
    void replaceFields_addsAndRemovesFields_returns200() throws Exception {
        var created = objectMapper.readTree(
            mockMvc.perform(post("/api/lists")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(restaurantListJson()))
                .andReturn().getResponse().getContentAsString());
        var id = created.get("id").asLong();

        var newFields = objectMapper.writeValueAsString(List.of(
            Map.of("name", "Rating", "type", "NUMBER", "required", false, "displayOrder", 0, "choices", List.of()),
            Map.of("name", "Notes", "type", "TEXT", "required", false, "displayOrder", 1, "choices", List.of())
        ));

        mockMvc.perform(put("/api/lists/{id}/fields", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newFields))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fields.length()").value(2))
            .andExpect(jsonPath("$.fields[0].name").value("Rating"))
            .andExpect(jsonPath("$.fields[1].name").value("Notes"));
    }

    @Test
    void deleteList_returns204_andSubsequentGetReturns404() throws Exception {
        var created = objectMapper.readTree(
            mockMvc.perform(post("/api/lists")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(restaurantListJson()))
                .andReturn().getResponse().getContentAsString());
        var id = created.get("id").asLong();

        mockMvc.perform(delete("/api/lists/{id}", id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lists/{id}", id))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteList_unknownId_returns404() throws Exception {
        mockMvc.perform(delete("/api/lists/99999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void replaceFields_unknownId_returns404() throws Exception {
        var fields = objectMapper.writeValueAsString(List.of(
            Map.of("name", "X", "type", "TEXT", "required", false, "displayOrder", 0, "choices", List.of())
        ));
        mockMvc.perform(put("/api/lists/99999/fields")
                .contentType(MediaType.APPLICATION_JSON)
                .content(fields))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateList_unknownId_returns404() throws Exception {
        var update = objectMapper.writeValueAsString(Map.of(
            "name", "X", "description", "", "icon", ""
        ));
        mockMvc.perform(put("/api/lists/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(update))
            .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPTION", "MULTI_OPTION"})
    void createList_choiceFieldWithNoChoices_returns400(String fieldType) throws Exception {
        var request = Map.of(
            "name", "List",
            "fields", List.of(Map.of(
                "name", "Field", "type", fieldType, "required", false, "displayOrder", 0, "choices", List.of()
            ))
        );

        mockMvc.perform(post("/api/lists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}

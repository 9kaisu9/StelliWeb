package com.stelli.stelli_backend.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class FileApiTest {

    @TempDir
    static Path uploadsRoot;

    @DynamicPropertySource
    static void uploadsPath(DynamicPropertyRegistry registry) {
        registry.add("app.uploads-path", () -> uploadsRoot.toString());
    }

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    tools.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    FileStorageService fileStorageService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void upload_storesFile_returnsRelativePath() throws Exception {
        var file = new MockMultipartFile("file", "poster.jpg", "image/jpeg", "the-bytes".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("listId", "1")
                .param("entryId", "2")
                .param("fieldId", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.path").value("1/2/3/poster.jpg"));
    }

    @Test
    void getFile_byReturnedPath_servesStoredBytes() throws Exception {
        var file = new MockMultipartFile("file", "clip.mp4", "video/mp4", "video-content".getBytes());
        var path = upload(file, 4, 5, 6);

        mockMvc.perform(get("/api/files/" + path))
            .andExpect(status().isOk())
            .andExpect(content().bytes("video-content".getBytes()));
    }

    @Test
    void imageFieldValue_roundTripsThroughEntry() throws Exception {
        // A List with a single IMAGE field, plus an Entry to attach the file to.
        var listId = createListWithImageField();
        var fieldId = getFirstFieldId(listId);
        var entryId = createEntry(listId, Map.of());

        // Upload the file under that list/entry/field, then store the returned path as the field value.
        var file = new MockMultipartFile("file", "poster.png", "image/png", "image-bytes".getBytes());
        var path = upload(file, listId, entryId, fieldId);
        updateEntry(listId, entryId, Map.of(String.valueOf(fieldId), path));

        // Retrieving the Entry returns the stored path...
        mockMvc.perform(get("/api/lists/{listId}/entries/{id}", listId, entryId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fieldValues." + fieldId).value(path));

        // ...and the file is fetchable by that path.
        mockMvc.perform(get("/api/files/" + path))
            .andExpect(status().isOk())
            .andExpect(content().bytes("image-bytes".getBytes()));
    }

    @Test
    void getFile_setsContentTypeFromExtension() throws Exception {
        var file = new MockMultipartFile("file", "poster.png", "image/png", "png-bytes".getBytes());
        var path = upload(file, 10, 11, 12);

        mockMvc.perform(get("/api/files/" + path))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void uploadedFile_persistsOnDisk_underUploadsRoot() throws Exception {
        var file = new MockMultipartFile("file", "memo.jpg", "image/jpeg", "durable".getBytes());
        var path = upload(file, 7, 8, 9);

        // The file lives on the configured uploads directory (a bind-mounted host dir in
        // production), so a fresh read off disk — i.e. what a restarted container sees —
        // still returns the stored bytes, independent of any in-memory or DB state.
        var onDisk = uploadsRoot.resolve(path);
        org.junit.jupiter.api.Assertions.assertArrayEquals(
            "durable".getBytes(), java.nio.file.Files.readAllBytes(onDisk));
    }

    @Test
    void load_rejectsPathTraversal_outsideUploadsRoot() throws Exception {
        // A real, readable file sitting just outside the uploads root.
        var secret = uploadsRoot.getParent().resolve("secret.txt");
        java.nio.file.Files.writeString(secret, "top-secret");

        var ex = org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> fileStorageService.load("../secret.txt"));
        org.junit.jupiter.api.Assertions.assertEquals(
            org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private long createListWithImageField() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
            "name", "Movies",
            "description", "Films to watch",
            "icon", "🎬",
            "fields", List.of(
                Map.of("name", "Poster", "type", "IMAGE", "required", false, "displayOrder", 0, "choices", List.of())
            )
        ));
        var result = mockMvc.perform(post("/api/lists")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long getFirstFieldId(long listId) throws Exception {
        var result = mockMvc.perform(get("/api/lists/{id}", listId)).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("fields").get(0).get("id").asLong();
    }

    private long createEntry(long listId, Map<String, Object> fieldValues) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("fieldValues", fieldValues));
        var json = mockMvc.perform(post("/api/lists/{listId}/entries", listId)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private void updateEntry(long listId, long entryId, Map<String, Object> fieldValues) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("fieldValues", fieldValues));
        mockMvc.perform(put("/api/lists/{listId}/entries/{id}", listId, entryId)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
    }

    private String upload(MockMultipartFile file, long listId, long entryId, long fieldId) throws Exception {
        var json = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("listId", String.valueOf(listId))
                .param("entryId", String.valueOf(entryId))
                .param("fieldId", String.valueOf(fieldId)))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("path").asText();
    }
}

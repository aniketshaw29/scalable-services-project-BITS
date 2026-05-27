package com.campuseventhub.resource;

import com.campuseventhub.resource.dto.ResourceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ResourceServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdId;
    private static String createdFileName;

    private MockMultipartFile sampleFile(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("file", filename, contentType, content);
    }

    @Test
    @Order(1)
    void uploadFile_success() throws Exception {
        MockMultipartFile file = sampleFile("slides.pdf", "application/pdf",
                "PDF content here".getBytes());

        String body = mockMvc.perform(multipart("/api/resources/upload")
                        .file(file)
                        .param("eventId", "10")
                        .param("uploadedBy", "admin")
                        .param("description", "Event slides"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("slides.pdf"))
                .andExpect(jsonPath("$.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.eventId").value(10))
                .andReturn().getResponse().getContentAsString();

        ResourceResponse resp = objectMapper.readValue(body, ResourceResponse.class);
        createdId = resp.getId();
        createdFileName = resp.getFileName();
    }

    @Test
    @Order(2)
    void uploadFile_textFile_success() throws Exception {
        MockMultipartFile file = sampleFile("notes.txt", "text/plain",
                "Some notes about the event".getBytes());

        mockMvc.perform(multipart("/api/resources/upload")
                        .file(file)
                        .param("eventId", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("notes.txt"));
    }

    @Test
    @Order(3)
    void getById_success() throws Exception {
        mockMvc.perform(get("/api/resources/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("slides.pdf"))
                .andExpect(jsonPath("$.uploadedBy").value("admin"));
    }

    @Test
    @Order(4)
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/resources/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void getByEvent_success() throws Exception {
        mockMvc.perform(get("/api/resources/event/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(6)
    void downloadFile_success() throws Exception {
        mockMvc.perform(get("/api/resources/" + createdId + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"slides.pdf\""));
    }

    @Test
    @Order(7)
    void uploadFile_exceedsLimit_returns413() throws Exception {
        // Create a byte array larger than 10MB
        byte[] bigContent = new byte[11 * 1024 * 1024];
        MockMultipartFile bigFile = sampleFile("big.bin", "application/octet-stream", bigContent);

        mockMvc.perform(multipart("/api/resources/upload")
                        .file(bigFile)
                        .param("eventId", "10"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Order(8)
    void deleteResource_success() throws Exception {
        mockMvc.perform(delete("/api/resources/" + createdId))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(9)
    void getById_afterDelete_returns404() throws Exception {
        mockMvc.perform(get("/api/resources/" + createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(10)
    void getByEvent_afterDelete_oneRemaining() throws Exception {
        mockMvc.perform(get("/api/resources/event/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Order(11)
    void getByEvent_noResources_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/resources/event/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

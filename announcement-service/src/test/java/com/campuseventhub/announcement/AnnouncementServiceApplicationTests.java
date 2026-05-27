package com.campuseventhub.announcement;

import com.campuseventhub.announcement.dto.AnnouncementRequest;
import com.campuseventhub.announcement.dto.AnnouncementResponse;
import com.campuseventhub.announcement.entity.AnnouncementType;
import com.campuseventhub.announcement.messaging.AnnouncementEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AnnouncementServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private AnnouncementEventPublisher announcementEventPublisher;

    private static Long createdId;

    private AnnouncementRequest buildRequest(String title, AnnouncementType type) {
        return AnnouncementRequest.builder()
                .title(title)
                .content("This is an important announcement about the upcoming event.")
                .eventId(10L)
                .type(type)
                .publishedBy("admin")
                .build();
    }

    @Test
    @Order(1)
    void createAnnouncement_success() throws Exception {
        doNothing().when(announcementEventPublisher).publishAnnouncementCreated(any());

        String body = mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Venue Change Notice", AnnouncementType.VENUE_CHANGE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Venue Change Notice"))
                .andExpect(jsonPath("$.type").value("VENUE_CHANGE"))
                .andReturn().getResponse().getContentAsString();

        createdId = objectMapper.readValue(body, AnnouncementResponse.class).getId();
    }

    @Test
    @Order(2)
    void createAnnouncement_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Missing title\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    @Order(3)
    void getById_success() throws Exception {
        mockMvc.perform(get("/api/announcements/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Venue Change Notice"));
    }

    @Test
    @Order(4)
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/announcements/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void getAll_success() throws Exception {
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Venue Change Notice"));
    }

    @Test
    @Order(6)
    void createGeneralAnnouncement_defaultType() throws Exception {
        doNothing().when(announcementEventPublisher).publishAnnouncementCreated(any());

        AnnouncementRequest req = AnnouncementRequest.builder()
                .title("General Info")
                .content("Just a general announcement.")
                .publishedBy("admin")
                .build();

        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("GENERAL"));
    }

    @Test
    @Order(7)
    void getByEvent_success() throws Exception {
        mockMvc.perform(get("/api/announcements/event/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventId").value(10));
    }

    @Test
    @Order(8)
    void getByType_venueChange() throws Exception {
        mockMvc.perform(get("/api/announcements/type/VENUE_CHANGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].type").value("VENUE_CHANGE"));
    }

    @Test
    @Order(9)
    void getAll_multipleAnnouncements() throws Exception {
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(10)
    void getByEvent_noAnnouncements_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/announcements/event/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

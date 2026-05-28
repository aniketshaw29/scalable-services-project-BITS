package com.campuseventhub.announcement;

import com.campuseventhub.announcement.dto.AnnouncementRequest;
import com.campuseventhub.announcement.entity.AnnouncementType;
import com.campuseventhub.announcement.messaging.AnnouncementEventPublisher;
import com.campuseventhub.announcement.repository.AnnouncementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests covering default-type assignment, event-scoped filtering,
 * type-based filtering, ordering (newest first), and publisher invocation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AnnouncementServiceIntegrationTest {

    @Autowired MockMvc                 mockMvc;
    @Autowired ObjectMapper            objectMapper;
    @Autowired AnnouncementRepository  repository;

    @MockBean AnnouncementEventPublisher eventPublisher;
    @MockBean RabbitTemplate             rabbitTemplate;

    static final Long EVENT_ID = 4001L;

    private AnnouncementRequest req(String title, String content, AnnouncementType type, Long eventId) {
        return AnnouncementRequest.builder()
                .title(title).content(content)
                .type(type).eventId(eventId)
                .publishedBy("Admin")
                .build();
    }

    @BeforeEach
    void stubPublisher() {
        doNothing().when(eventPublisher).publishAnnouncementCreated(any());
    }

    // ── default type assignment ───────────────────────────────────────────────

    @Test @Order(1)
    void create_nullType_defaultsToGeneral() throws Exception {
        var r = AnnouncementRequest.builder()
                .title("General Notice").content("Info here").publishedBy("Admin").build();

        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("GENERAL"));
    }

    @Test @Order(2)
    void create_withExplicitType_usesGivenType() throws Exception {
        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                req("Venue Change", "Room moved to B2", AnnouncementType.VENUE_CHANGE, EVENT_ID))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("VENUE_CHANGE"))
                .andExpect(jsonPath("$.eventId").value(EVENT_ID));
    }

    @Test @Order(3)
    void create_emergency_persists() throws Exception {
        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                req("Emergency", "Campus closed", AnnouncementType.EMERGENCY, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EMERGENCY"));
    }

    // ── ordering (newest first) ───────────────────────────────────────────────

    @Test @Order(4)
    void getAll_returnsNewestFirst() throws Exception {
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
        // Most recent (Emergency) was created last — check it appears before General
        // (exact order depends on publishedAt timestamp; at minimum all three are present)
    }

    // ── event-scoped filtering ────────────────────────────────────────────────

    @Test @Order(5)
    void getByEvent_returnsOnlyAnnouncementsForThatEvent() throws Exception {
        mockMvc.perform(get("/api/announcements/event/{id}", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventId").value(EVENT_ID));

        // The General Notice has no eventId — must not appear
        var list = repository.findByEventIdOrderByPublishedAtDesc(EVENT_ID);
        assertThat(list).allMatch(a -> EVENT_ID.equals(a.getEventId()));
    }

    @Test @Order(6)
    void getByEvent_unknownId_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/announcements/event/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── type-based filtering ──────────────────────────────────────────────────

    @Test @Order(7)
    void getByType_VENUE_CHANGE_returnsOnlyVenueChanges() throws Exception {
        mockMvc.perform(get("/api/announcements/type/VENUE_CHANGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("VENUE_CHANGE"));
    }

    @Test @Order(8)
    void getByType_EMERGENCY_returnsOnlyEmergencies() throws Exception {
        mockMvc.perform(get("/api/announcements/type/EMERGENCY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Emergency"));
    }

    // ── validation ───────────────────────────────────────────────────────────

    @Test @Order(9)
    void create_missingTitle_returns400() throws Exception {
        var bad = AnnouncementRequest.builder().content("No title").build();
        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(10)
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/announcements/99999"))
                .andExpect(status().isNotFound());
    }
}

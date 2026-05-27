package com.campuseventhub.event;

import com.campuseventhub.event.dto.CapacityUpdateRequest;
import com.campuseventhub.event.dto.EventRequest;
import com.campuseventhub.event.dto.EventResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventServiceApplicationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    static Long createdEventId;

    @Test @Order(1)
    void contextLoads() { }

    @Test @Order(2)
    void createEvent_returns201WithId() throws Exception {
        EventRequest req = EventRequest.builder()
                .title("Spring Boot Workshop")
                .description("Hands-on workshop")
                .eventDate(LocalDateTime.of(2024, 8, 15, 10, 0))
                .endDate(LocalDateTime.of(2024, 8, 15, 17, 0))
                .category("WORKSHOP")
                .maxCapacity(50)
                .build();

        MvcResult result = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Spring Boot Workshop"))
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andExpect(jsonPath("$.currentRegistrations").value(0))
                .andReturn();

        EventResponse resp = objectMapper.readValue(result.getResponse().getContentAsString(), EventResponse.class);
        createdEventId = resp.getId();
    }

    @Test @Order(3)
    void createEvent_missingTitle_returns400() throws Exception {
        EventRequest req = EventRequest.builder()
                .eventDate(LocalDateTime.now().plusDays(5))
                .maxCapacity(30)
                .build();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test @Order(4)
    void getAllEvents_returnsArray() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test @Order(5)
    void getEventById_returnsCorrectEvent() throws Exception {
        mockMvc.perform(get("/api/events/{id}", createdEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdEventId))
                .andExpect(jsonPath("$.title").value("Spring Boot Workshop"));
    }

    @Test @Order(6)
    void getEventById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/events/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("99999")));
    }

    @Test @Order(7)
    void getEventsByStatus_UPCOMING_returnsOnlyUpcoming() throws Exception {
        mockMvc.perform(get("/api/events/status/UPCOMING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("UPCOMING"));
    }

    @Test @Order(8)
    void updateEvent_returns200WithUpdatedTitle() throws Exception {
        EventRequest update = EventRequest.builder()
                .title("Spring Boot Workshop — Updated")
                .maxCapacity(60)
                .eventDate(LocalDateTime.of(2024, 8, 15, 10, 0))
                .build();

        mockMvc.perform(put("/api/events/{id}", createdEventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Spring Boot Workshop — Updated"))
                .andExpect(jsonPath("$.maxCapacity").value(60));
    }

    @Test @Order(9)
    void updateCapacity_increment_succeeds() throws Exception {
        CapacityUpdateRequest req = new CapacityUpdateRequest(1);

        mockMvc.perform(put("/api/events/{id}/capacity", createdEventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRegistrations").value(1));
    }

    @Test @Order(10)
    void updateCapacity_decrement_succeeds() throws Exception {
        CapacityUpdateRequest req = new CapacityUpdateRequest(-1);

        mockMvc.perform(put("/api/events/{id}/capacity", createdEventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentRegistrations").value(0));
    }

    @Test @Order(11)
    void updateCapacity_overMax_returns409() throws Exception {
        // Fill to max (60), then try one more
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(put("/api/events/{id}/capacity", createdEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CapacityUpdateRequest(1))))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(put("/api/events/{id}/capacity", createdEventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CapacityUpdateRequest(1))))
                .andExpect(status().isConflict());
    }

    @Test @Order(12)
    void deleteEvent_returns204() throws Exception {
        // Create a temporary event to delete
        EventRequest req = EventRequest.builder()
                .title("To Delete")
                .eventDate(LocalDateTime.now().plusDays(1))
                .maxCapacity(10)
                .build();
        MvcResult result = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        Long tempId = objectMapper.readValue(result.getResponse().getContentAsString(), EventResponse.class).getId();

        mockMvc.perform(delete("/api/events/{id}", tempId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/events/{id}", tempId))
                .andExpect(status().isNotFound());
    }
}

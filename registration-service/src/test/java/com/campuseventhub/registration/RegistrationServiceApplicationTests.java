package com.campuseventhub.registration;

import com.campuseventhub.registration.client.EventClient;
import com.campuseventhub.registration.client.EventDto;
import com.campuseventhub.registration.messaging.RegistrationEventPublisher;
import com.campuseventhub.registration.dto.RegistrationRequest;
import com.campuseventhub.registration.dto.RegistrationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RegistrationServiceApplicationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean EventClient eventClient;
    @MockBean RegistrationEventPublisher eventPublisher;

    static Long createdRegistrationId;

    static final Long EVENT_ID = 1L;
    static final String STUDENT_ID = "STU-001";

    private EventDto mockEvent(int currentRegistrations, int maxCapacity) {
        EventDto event = new EventDto();
        event.setId(EVENT_ID);
        event.setTitle("Test Event");
        event.setMaxCapacity(maxCapacity);
        event.setCurrentRegistrations(currentRegistrations);
        event.setStatus("UPCOMING");
        return event;
    }

    @Test @Order(1)
    void contextLoads() { }

    @Test @Order(2)
    void register_returns201WithId() throws Exception {
        when(eventClient.getEventById(EVENT_ID)).thenReturn(mockEvent(0, 50));
        when(eventClient.updateCapacity(eq(EVENT_ID), any(Map.class))).thenReturn(mockEvent(1, 50));

        RegistrationRequest req = RegistrationRequest.builder()
                .studentId(STUDENT_ID)
                .studentName("Alice Johnson")
                .studentEmail("alice@college.edu")
                .eventId(EVENT_ID)
                .build();

        MvcResult result = mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID))
                .andExpect(jsonPath("$.eventId").value(EVENT_ID))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        RegistrationResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), RegistrationResponse.class);
        createdRegistrationId = resp.getId();
    }

    @Test @Order(3)
    void register_missingStudentId_returns400() throws Exception {
        RegistrationRequest req = RegistrationRequest.builder()
                .studentName("Bob")
                .studentEmail("bob@college.edu")
                .eventId(EVENT_ID)
                .build();

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.studentId").exists());
    }

    @Test @Order(4)
    void register_invalidEmail_returns400() throws Exception {
        RegistrationRequest req = RegistrationRequest.builder()
                .studentId("STU-002")
                .studentName("Bob")
                .studentEmail("not-an-email")
                .eventId(EVENT_ID)
                .build();

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.studentEmail").exists());
    }

    @Test @Order(5)
    void register_duplicate_returns409() throws Exception {
        when(eventClient.getEventById(EVENT_ID)).thenReturn(mockEvent(1, 50));

        RegistrationRequest req = RegistrationRequest.builder()
                .studentId(STUDENT_ID)
                .studentName("Alice Johnson")
                .studentEmail("alice@college.edu")
                .eventId(EVENT_ID)
                .build();

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString(STUDENT_ID)));
    }

    @Test @Order(6)
    void register_eventAtCapacity_returns409() throws Exception {
        when(eventClient.getEventById(EVENT_ID)).thenReturn(mockEvent(50, 50));

        RegistrationRequest req = RegistrationRequest.builder()
                .studentId("STU-999")
                .studentName("New Student")
                .studentEmail("new@college.edu")
                .eventId(EVENT_ID)
                .build();

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("full capacity")));
    }

    @Test @Order(7)
    void getById_returnsRegistration() throws Exception {
        mockMvc.perform(get("/api/registrations/{id}", createdRegistrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdRegistrationId))
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID));
    }

    @Test @Order(8)
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/registrations/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("99999")));
    }

    @Test @Order(9)
    void getByEventId_returnsList() throws Exception {
        mockMvc.perform(get("/api/registrations/event/{eventId}", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].eventId").value(EVENT_ID));
    }

    @Test @Order(10)
    void getByStudentId_returnsList() throws Exception {
        mockMvc.perform(get("/api/registrations/student/{studentId}", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].studentId").value(STUDENT_ID));
    }

    @Test @Order(11)
    void checkExists_existingId_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/registrations/{id}/exists", createdRegistrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test @Order(12)
    void checkExists_missingId_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/registrations/99999/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test @Order(13)
    void cancelRegistration_returns204() throws Exception {
        when(eventClient.updateCapacity(eq(EVENT_ID), any(Map.class))).thenReturn(mockEvent(0, 50));

        mockMvc.perform(delete("/api/registrations/{id}", createdRegistrationId))
                .andExpect(status().isNoContent());
    }

    @Test @Order(14)
    void getById_afterCancel_statusIsCancelled() throws Exception {
        mockMvc.perform(get("/api/registrations/{id}", createdRegistrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}

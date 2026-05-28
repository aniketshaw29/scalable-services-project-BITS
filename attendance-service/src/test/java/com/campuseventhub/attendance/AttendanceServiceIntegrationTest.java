package com.campuseventhub.attendance;

import com.campuseventhub.attendance.client.ExistsResponse;
import com.campuseventhub.attendance.client.RegistrationClient;
import com.campuseventhub.attendance.dto.AttendanceRequest;
import com.campuseventhub.attendance.messaging.AttendanceEventPublisher;
import com.campuseventhub.attendance.repository.AttendanceRepository;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests covering the full mark-attendance flow:
 * Feign client interaction, event publishing, duplicate enforcement,
 * status check, and per-event / per-student list queries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AttendanceServiceIntegrationTest {

    @Autowired MockMvc              mockMvc;
    @Autowired ObjectMapper         objectMapper;
    @Autowired AttendanceRepository attendanceRepository;

    @MockBean RegistrationClient        registrationClient;
    @MockBean AttendanceEventPublisher  eventPublisher;
    @MockBean RabbitTemplate            rabbitTemplate;

    static final Long REG_1   = 6001L;
    static final Long REG_2   = 6002L;
    static final Long EVENT_1 = 7001L;
    static final String STU_1 = "STU-ATT-001";
    static final String STU_2 = "STU-ATT-002";

    private AttendanceRequest req(Long regId, String stuId, Long evtId) {
        return AttendanceRequest.builder()
                .registrationId(regId)
                .studentId(stuId)
                .studentName("Student " + stuId)
                .studentEmail(stuId.toLowerCase() + "@college.edu")
                .eventId(evtId)
                .eventTitle("Attendance Integration Event")
                .build();
    }

    @BeforeEach
    void stubMocks() {
        when(registrationClient.checkExists(REG_1)).thenReturn(new ExistsResponse(true));
        when(registrationClient.checkExists(REG_2)).thenReturn(new ExistsResponse(true));
        doNothing().when(eventPublisher).publishAttendanceCompleted(any());
    }

    // ── mark attendance persists row and fires event ──────────────────────────

    @Test @Order(1)
    void markAttendance_success_persistsRowAndFiresEvent() throws Exception {
        mockMvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req(REG_1, STU_1, EVENT_1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationId").value(REG_1))
                .andExpect(jsonPath("$.status").value("PRESENT"));

        assertThat(attendanceRepository.existsByRegistrationId(REG_1)).isTrue();
        verify(eventPublisher, times(1)).publishAttendanceCompleted(any());
    }

    @Test @Order(3)
    void markSecondStudent_success() throws Exception {
        mockMvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req(REG_2, STU_2, EVENT_1))))
                .andExpect(status().isCreated());

        assertThat(attendanceRepository.count()).isEqualTo(2);
    }

    // ── duplicate enforcement ─────────────────────────────────────────────────

    @Test @Order(4)
    void markAttendance_duplicate_returns409() throws Exception {
        mockMvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req(REG_1, STU_1, EVENT_1))))
                .andExpect(status().isConflict());
    }

    // ── registration not found ────────────────────────────────────────────────

    @Test @Order(5)
    void markAttendance_unknownRegistration_returns404() throws Exception {
        when(registrationClient.checkExists(9999L)).thenReturn(new ExistsResponse(false));

        mockMvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req(9999L, "STU-X", EVENT_1))))
                .andExpect(status().isNotFound());
    }

    // ── status check ─────────────────────────────────────────────────────────

    @Test @Order(6)
    void statusCheck_marked_returnsPresent() throws Exception {
        mockMvc.perform(get("/api/attendance/{id}/status", REG_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.present").value(true))
                .andExpect(jsonPath("$.markedAt").isNotEmpty());
    }

    @Test @Order(7)
    void statusCheck_notMarked_returnsAbsent() throws Exception {
        mockMvc.perform(get("/api/attendance/{id}/status", 9998L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.present").value(false));
    }

    // ── list queries ──────────────────────────────────────────────────────────

    @Test @Order(8)
    void getByEvent_returnsBothAttendees() throws Exception {
        mockMvc.perform(get("/api/attendance/event/{id}", EVENT_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test @Order(9)
    void getByStudent_returnsOnlyThatStudent() throws Exception {
        mockMvc.perform(get("/api/attendance/student/{id}", STU_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentId").value(STU_1));
    }

    // ── validation ───────────────────────────────────────────────────────────

    @Test @Order(10)
    void missingRegistrationId_returns400() throws Exception {
        var bad = AttendanceRequest.builder()
                .studentId(STU_1).eventId(EVENT_1).build();
        mockMvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }
}

package com.campuseventhub.leaderboard;

import com.campuseventhub.leaderboard.dto.ResultRequest;
import com.campuseventhub.leaderboard.entity.Position;
import com.campuseventhub.leaderboard.messaging.ResultsEventPublisher;
import com.campuseventhub.leaderboard.repository.ResultRepository;
import com.campuseventhub.leaderboard.service.LeaderboardService;
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
 * Integration tests covering auto-point assignment, custom points,
 * top-performers aggregation, and per-event ranking order.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LeaderboardServiceIntegrationTest {

    @Autowired MockMvc            mockMvc;
    @Autowired ObjectMapper       objectMapper;
    @Autowired LeaderboardService leaderboardService;
    @Autowired ResultRepository   resultRepository;

    @MockBean RabbitTemplate       rabbitTemplate;
    @MockBean ResultsEventPublisher eventPublisher;

    static final Long EVENT_ID  = 3001L;
    static final Long EVENT_ID2 = 3002L;

    private ResultRequest req(Long eventId, String studentId, String name, Position pos) {
        return ResultRequest.builder()
                .eventId(eventId).eventTitle("Integration Event")
                .studentId(studentId).studentName(name)
                .position(pos).build();
    }

    @BeforeEach
    void stubPublisher() {
        doNothing().when(eventPublisher).publishResults(any());
    }

    // ── auto-point assignment ─────────────────────────────────────────────────

    @Test @Order(1)
    void publishResult_first_autoAssigns100Points() {
        var r = leaderboardService.publishResult(req(EVENT_ID, "STU-LB-01", "Alice", Position.FIRST));
        assertThat(r.getPoints()).isEqualTo(100);
    }

    @Test @Order(2)
    void publishResult_second_autoAssigns75Points() {
        var r = leaderboardService.publishResult(req(EVENT_ID, "STU-LB-02", "Bob", Position.SECOND));
        assertThat(r.getPoints()).isEqualTo(75);
    }

    @Test @Order(3)
    void publishResult_third_autoAssigns50Points() {
        var r = leaderboardService.publishResult(req(EVENT_ID, "STU-LB-03", "Carol", Position.THIRD));
        assertThat(r.getPoints()).isEqualTo(50);
    }

    @Test @Order(4)
    void publishResult_participant_autoAssigns10Points() {
        var r = leaderboardService.publishResult(req(EVENT_ID, "STU-LB-04", "Dave", Position.PARTICIPANT));
        assertThat(r.getPoints()).isEqualTo(10);
    }

    @Test @Order(5)
    void publishResult_customPoints_overridesAutoAssignment() {
        var customReq = ResultRequest.builder()
                .eventId(EVENT_ID2).eventTitle("Custom Event")
                .studentId("STU-LB-05").studentName("Eve")
                .position(Position.FIRST).points(200).build();
        var r = leaderboardService.publishResult(customReq);
        assertThat(r.getPoints()).isEqualTo(200);
    }

    // ── per-event ranking order ───────────────────────────────────────────────

    @Test @Order(6)
    void getResultsByEvent_returnsSortedByPointsDesc() throws Exception {
        mockMvc.perform(get("/api/leaderboard/event/{id}", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].points").value(100))
                .andExpect(jsonPath("$[1].points").value(75))
                .andExpect(jsonPath("$[2].points").value(50))
                .andExpect(jsonPath("$[3].points").value(10));
    }

    @Test @Order(7)
    void getResultsByEvent_empty_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/leaderboard/event/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── top performers aggregation ────────────────────────────────────────────

    @Test @Order(8)
    void topPerformers_aliceIsFirst_with100Points() throws Exception {
        mockMvc.perform(get("/api/leaderboard/top").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Eve"))   // 200 pts from custom
                .andExpect(jsonPath("$[1].studentName").value("Alice")); // 100 pts
    }

    @Test @Order(9)
    void topPerformers_limitIsRespected() throws Exception {
        mockMvc.perform(get("/api/leaderboard/top").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ── per-student retrieval ─────────────────────────────────────────────────

    @Test @Order(10)
    void getResultsByStudent_returnsOnlyThatStudentsResults() throws Exception {
        mockMvc.perform(get("/api/leaderboard/student/{id}", "STU-LB-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].position").value("FIRST"));
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test @Order(11)
    void missingStudentId_returns400() throws Exception {
        var bad = ResultRequest.builder().eventId(EVENT_ID).position(Position.FIRST).build();
        mockMvc.perform(post("/api/leaderboard/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }
}

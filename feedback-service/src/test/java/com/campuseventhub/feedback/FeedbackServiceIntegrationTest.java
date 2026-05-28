package com.campuseventhub.feedback;

import com.campuseventhub.feedback.dto.FeedbackRequest;
import com.campuseventhub.feedback.repository.FeedbackRepository;
import com.campuseventhub.feedback.service.FeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests covering service + repository + HTTP layer together:
 * summary aggregation logic, duplicate enforcement, cross-student isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FeedbackServiceIntegrationTest {

    @Autowired MockMvc         mockMvc;
    @Autowired ObjectMapper    objectMapper;
    @Autowired FeedbackService feedbackService;
    @Autowired FeedbackRepository repository;

    static final Long EVENT_A = 2001L;
    static final Long EVENT_B = 2002L;

    private FeedbackRequest req(String studentId, Long eventId, int rating, String comment) {
        return FeedbackRequest.builder()
                .studentId(studentId).studentName("Student " + studentId)
                .eventId(eventId).rating(rating).comment(comment).build();
    }

    // ── summary computation ───────────────────────────────────────────────────

    @Test @Order(1)
    void summary_emptyEvent_returnsZeroAvg() throws Exception {
        mockMvc.perform(get("/api/feedback/event/{id}/summary", 9999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.totalResponses").value(0));
    }

    @Test @Order(2)
    void summary_singleRating_avgEqualsThatRating() {
        feedbackService.submitFeedback(req("STU-FB-01", EVENT_A, 4, "Good event"));

        var summary = feedbackService.getSummaryByEvent(EVENT_A);
        assertThat(summary.getAverageRating()).isEqualTo(4.0);
        assertThat(summary.getTotalResponses()).isEqualTo(1);
    }

    @Test @Order(3)
    void summary_multipleRatings_avgCorrectlyComputed() {
        feedbackService.submitFeedback(req("STU-FB-02", EVENT_A, 2, "Could be better"));
        feedbackService.submitFeedback(req("STU-FB-03", EVENT_A, 5, "Excellent!"));

        // ratings: 4, 2, 5  → avg = 3.7 (rounded to 1 decimal)
        var summary = feedbackService.getSummaryByEvent(EVENT_A);
        assertThat(summary.getTotalResponses()).isEqualTo(3);
        assertThat(summary.getAverageRating()).isEqualTo(3.7);
    }

    @Test @Order(4)
    void summary_distributionContainsAllSubmittedRatings() {
        var summary = feedbackService.getSummaryByEvent(EVENT_A);
        assertThat(summary.getRatingDistribution()).containsKeys(2, 4, 5);
        assertThat(summary.getRatingDistribution().get(5)).isEqualTo(1L);
    }

    @Test @Order(5)
    void summary_viaHttp_matchesServiceResult() throws Exception {
        mockMvc.perform(get("/api/feedback/event/{id}/summary", EVENT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResponses").value(3))
                .andExpect(jsonPath("$.averageRating").value(greaterThan(0.0)));
    }

    // ── duplicate enforcement ─────────────────────────────────────────────────

    @Test @Order(6)
    void duplicateFeedback_sameStudentSameEvent_returns409() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                req("STU-FB-01", EVENT_A, 3, "Trying again"))))
                .andExpect(status().isConflict());
    }

    @Test @Order(7)
    void sameStudentDifferentEvent_allowed() {
        feedbackService.submitFeedback(req("STU-FB-01", EVENT_B, 5, "Different event, great!"));

        var list = feedbackService.getFeedbackByStudent("STU-FB-01");
        assertThat(list).hasSize(2);
        assertThat(list).extracting("eventId").contains(EVENT_A, EVENT_B);
    }

    // ── cross-event isolation ────────────────────────────────────────────────

    @Test @Order(8)
    void feedbackForEventB_doesNotAppearInEventA_results() throws Exception {
        mockMvc.perform(get("/api/feedback/event/{id}", EVENT_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(EVENT_B));

        // Event A should still only have 3 entries
        mockMvc.perform(get("/api/feedback/event/{id}", EVENT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    // ── validation ───────────────────────────────────────────────────────────

    @Test @Order(9)
    void ratingOutOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                req("STU-FB-NEW", EVENT_B, 6, "too high"))))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(10)
    void missingStudentId_returns400() throws Exception {
        var bad = FeedbackRequest.builder().eventId(EVENT_A).rating(3).build();
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }
}

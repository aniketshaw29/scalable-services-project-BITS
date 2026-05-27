package com.campuseventhub.feedback;

import com.campuseventhub.feedback.dto.FeedbackRequest;
import com.campuseventhub.feedback.dto.FeedbackResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class FeedbackServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdId;

    private FeedbackRequest buildRequest(String studentId, Long eventId, int rating) {
        return FeedbackRequest.builder()
                .studentId(studentId)
                .studentName("Alice Smith")
                .eventId(eventId)
                .rating(rating)
                .comment("Great event!")
                .build();
    }

    @Test
    @Order(1)
    void submitFeedback_success() throws Exception {
        String body = mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("S001", 10L, 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value("S001"))
                .andExpect(jsonPath("$.rating").value(5))
                .andReturn().getResponse().getContentAsString();

        createdId = objectMapper.readValue(body, FeedbackResponse.class).getId();
    }

    @Test
    @Order(2)
    void submitFeedback_duplicate_returns409() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("S001", 10L, 4))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @Order(3)
    void submitFeedback_ratingOutOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("S002", 10L, 6))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rating").exists());
    }

    @Test
    @Order(4)
    void submitFeedback_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void getById_success() throws Exception {
        mockMvc.perform(get("/api/feedback/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    @Order(6)
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/feedback/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    void getByEvent_success() throws Exception {
        mockMvc.perform(get("/api/feedback/event/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventId").value(10));
    }

    @Test
    @Order(8)
    void getSummary_singleResponse() throws Exception {
        mockMvc.perform(get("/api/feedback/event/10/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResponses").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.ratingDistribution.5").value(1));
    }

    @Test
    @Order(9)
    void submitSecondFeedback_differentStudent() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("S002", 10L, 3))))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(10)
    void getSummary_multipleResponses_averageCorrect() throws Exception {
        mockMvc.perform(get("/api/feedback/event/10/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResponses").value(2))
                .andExpect(jsonPath("$.averageRating").value(4.0));
    }

    @Test
    @Order(11)
    void getByStudent_success() throws Exception {
        mockMvc.perform(get("/api/feedback/student/S001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].studentId").value("S001"));
    }

    @Test
    @Order(12)
    void getSummary_noFeedback_returnsZero() throws Exception {
        mockMvc.perform(get("/api/feedback/event/999/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResponses").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0));
    }
}

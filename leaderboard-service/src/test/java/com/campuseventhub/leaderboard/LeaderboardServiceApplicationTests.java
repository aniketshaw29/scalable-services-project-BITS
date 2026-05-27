package com.campuseventhub.leaderboard;

import com.campuseventhub.leaderboard.dto.ResultRequest;
import com.campuseventhub.leaderboard.dto.ResultResponse;
import com.campuseventhub.leaderboard.entity.Position;
import com.campuseventhub.leaderboard.messaging.ResultsEventPublisher;
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
class LeaderboardServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private ResultsEventPublisher resultsEventPublisher;

    private static Long createdId;

    private ResultRequest buildRequest(String studentId, Position position) {
        return ResultRequest.builder()
                .eventId(20L)
                .eventTitle("Hackathon 2024")
                .studentId(studentId)
                .studentName("Alice Smith")
                .position(position)
                .category("Web Dev")
                .build();
    }

    @Test
    @Order(1)
    void publishResult_first_place() throws Exception {
        doNothing().when(resultsEventPublisher).publishResults(any());

        String body = mockMvc.perform(post("/api/leaderboard/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("S001", Position.FIRST))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value("FIRST"))
                .andExpect(jsonPath("$.points").value(100))
                .andReturn().getResponse().getContentAsString();

        createdId = objectMapper.readValue(body, ResultResponse.class).getId();
    }

    @Test
    @Order(2)
    void publishResult_second_place() throws Exception {
        doNothing().when(resultsEventPublisher).publishResults(any());

        mockMvc.perform(post("/api/leaderboard/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("S002", Position.SECOND))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.points").value(75));
    }

    @Test
    @Order(3)
    void publishResult_participant_defaultPoints() throws Exception {
        doNothing().when(resultsEventPublisher).publishResults(any());

        mockMvc.perform(post("/api/leaderboard/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("S003", Position.PARTICIPANT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.points").value(10));
    }

    @Test
    @Order(4)
    void publishResult_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/leaderboard/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void getById_success() throws Exception {
        mockMvc.perform(get("/api/leaderboard/results/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("S001"));
    }

    @Test
    @Order(6)
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/leaderboard/results/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    void getByEvent_sortedByPoints() throws Exception {
        mockMvc.perform(get("/api/leaderboard/event/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].points").value(100));
    }

    @Test
    @Order(8)
    void getByStudent_success() throws Exception {
        mockMvc.perform(get("/api/leaderboard/student/S001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value("S001"));
    }

    @Test
    @Order(9)
    void getTopPerformers_defaultLimit() throws Exception {
        mockMvc.perform(get("/api/leaderboard/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].points").value(100));
    }

    @Test
    @Order(10)
    void getTopPerformers_limitOne() throws Exception {
        mockMvc.perform(get("/api/leaderboard/top?limit=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Order(11)
    void getByEvent_empty() throws Exception {
        mockMvc.perform(get("/api/leaderboard/event/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

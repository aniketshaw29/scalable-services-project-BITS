package com.campuseventhub.sponsor;

import com.campuseventhub.sponsor.dto.*;
import com.campuseventhub.sponsor.entity.SponsorTier;
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
class SponsorServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdSponsorId;

    private SponsorRequest buildRequest(String name, SponsorTier tier) {
        return SponsorRequest.builder()
                .name(name)
                .website("https://example.com")
                .tier(tier)
                .contactPerson("John Doe")
                .contactEmail("john@example.com")
                .description("A great sponsor")
                .build();
    }

    @Test
    @Order(1)
    void createSponsor_success() throws Exception {
        String body = mockMvc.perform(post("/api/sponsors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("TechCorp", SponsorTier.GOLD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("TechCorp"))
                .andExpect(jsonPath("$.tier").value("GOLD"))
                .andReturn().getResponse().getContentAsString();

        createdSponsorId = objectMapper.readValue(body, SponsorResponse.class).getId();
    }

    @Test
    @Order(2)
    void createSponsor_defaultTier() throws Exception {
        SponsorRequest req = SponsorRequest.builder().name("SmallCo").build();

        mockMvc.perform(post("/api/sponsors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tier").value("BRONZE"));
    }

    @Test
    @Order(3)
    void createSponsor_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/sponsors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    @Order(4)
    void getById_success() throws Exception {
        mockMvc.perform(get("/api/sponsors/" + createdSponsorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TechCorp"));
    }

    @Test
    @Order(5)
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/sponsors/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    void getAll_success() throws Exception {
        mockMvc.perform(get("/api/sponsors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(7)
    void updateSponsor_success() throws Exception {
        SponsorRequest update = buildRequest("TechCorp Updated", SponsorTier.PLATINUM);

        mockMvc.perform(put("/api/sponsors/" + createdSponsorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TechCorp Updated"))
                .andExpect(jsonPath("$.tier").value("PLATINUM"));
    }

    @Test
    @Order(8)
    void linkSponsorToEvent_success() throws Exception {
        LinkSponsorRequest link = LinkSponsorRequest.builder()
                .notes("Premier sponsor of the event")
                .build();

        mockMvc.perform(post("/api/sponsors/" + createdSponsorId + "/events/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(link)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(20))
                .andExpect(jsonPath("$.sponsor.id").value(createdSponsorId));
    }

    @Test
    @Order(9)
    void linkSponsorToEvent_duplicate_returns409() throws Exception {
        mockMvc.perform(post("/api/sponsors/" + createdSponsorId + "/events/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @Order(10)
    void getSponsorsByEvent_success() throws Exception {
        mockMvc.perform(get("/api/sponsors/event/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventId").value(20))
                .andExpect(jsonPath("$[0].sponsor.name").value("TechCorp Updated"));
    }

    @Test
    @Order(11)
    void getSponsorsByEvent_empty() throws Exception {
        mockMvc.perform(get("/api/sponsors/event/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(12)
    void linkSponsorToEvent_sponsorNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/sponsors/99999/events/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }
}

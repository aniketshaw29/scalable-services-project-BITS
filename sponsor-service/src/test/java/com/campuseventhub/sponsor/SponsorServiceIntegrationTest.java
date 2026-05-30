package com.campuseventhub.sponsor;

import com.campuseventhub.sponsor.dto.LinkSponsorRequest;
import com.campuseventhub.sponsor.dto.SponsorRequest;
import com.campuseventhub.sponsor.entity.SponsorTier;
import com.campuseventhub.sponsor.repository.EventSponsorRepository;
import com.campuseventhub.sponsor.repository.SponsorRepository;
import com.campuseventhub.sponsor.service.SponsorService;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests covering: default tier assignment, update, event linking,
 * duplicate link enforcement, and per-event sponsor retrieval.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SponsorServiceIntegrationTest {

    @Autowired MockMvc               mockMvc;
    @Autowired ObjectMapper          objectMapper;
    @Autowired SponsorService        sponsorService;
    @Autowired SponsorRepository     sponsorRepository;
    @Autowired EventSponsorRepository eventSponsorRepository;

    static final Long EVENT_ID  = 5001L;
    static final Long EVENT_ID2 = 5002L;

    static Long sponsorAId;
    static Long sponsorBId;

    private SponsorRequest req(String name, SponsorTier tier) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        return SponsorRequest.builder()
                .name(name).tier(tier)
                .website("https://" + slug + ".com")
                .contactPerson("Contact").contactEmail("contact@" + slug + ".com")
                .build();
    }

    // ── create + default tier ─────────────────────────────────────────────────

    @Test @Order(1)
    void createSponsor_withTier_persistsCorrectTier() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sponsors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req("AcmeCorp", SponsorTier.PLATINUM))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tier").value("PLATINUM"))
                .andExpect(jsonPath("$.name").value("AcmeCorp"))
                .andReturn();

        sponsorAId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test @Order(2)
    void createSponsor_nullTier_defaultsToBronze() throws Exception {
        var noTier = SponsorRequest.builder()
                .name("StartupX")
                .website("https://startupx.io")
                .contactEmail("hi@startupx.io")
                .build();

        MvcResult result = mockMvc.perform(post("/api/sponsors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noTier)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tier").value("BRONZE"))
                .andReturn();

        sponsorBId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test @Order(3)
    void updateSponsor_changesTierAndName() throws Exception {
        var update = req("AcmeCorp Updated", SponsorTier.GOLD);

        mockMvc.perform(put("/api/sponsors/{id}", sponsorAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("AcmeCorp Updated"))
                .andExpect(jsonPath("$.tier").value("GOLD"));

        assertThat(sponsorRepository.findById(sponsorAId).orElseThrow().getTier())
                .isEqualTo(SponsorTier.GOLD);
    }

    // ── linking ───────────────────────────────────────────────────────────────

    @Test @Order(4)
    void linkSponsorToEvent_success_persistsLink() throws Exception {
        var link = LinkSponsorRequest.builder()
                .contribution(new BigDecimal("50000.00"))
                .notes("Gold sponsor for this event")
                .build();

        mockMvc.perform(post("/api/sponsors/{sponsorId}/events/{eventId}", sponsorAId, EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(link)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(EVENT_ID))
                .andExpect(jsonPath("$.contribution").value(50000.00));

        assertThat(eventSponsorRepository.existsByEventIdAndSponsorId(EVENT_ID, sponsorAId)).isTrue();
    }

    @Test @Order(5)
    void linkSponsor_secondSponsorToSameEvent() throws Exception {
        mockMvc.perform(post("/api/sponsors/{sponsorId}/events/{eventId}", sponsorBId, EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkSponsorRequest())))
                .andExpect(status().isCreated());
    }

    @Test @Order(6)
    void linkSponsor_duplicate_returns409() throws Exception {
        mockMvc.perform(post("/api/sponsors/{sponsorId}/events/{eventId}", sponsorAId, EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkSponsorRequest())))
                .andExpect(status().isConflict());
    }

    @Test @Order(7)
    void linkSponsor_sameSponsorDifferentEvent_allowed() throws Exception {
        mockMvc.perform(post("/api/sponsors/{sponsorId}/events/{eventId}", sponsorAId, EVENT_ID2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkSponsorRequest())))
                .andExpect(status().isCreated());
    }

    // ── per-event retrieval ───────────────────────────────────────────────────

    @Test @Order(8)
    void getSponsorsByEvent_returnsBothLinkedSponsors() throws Exception {
        mockMvc.perform(get("/api/sponsors/event/{id}", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].sponsor.name",
                        containsInAnyOrder("AcmeCorp Updated", "StartupX")));
    }

    @Test @Order(9)
    void getSponsorsByEvent_unknownEvent_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/sponsors/event/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── list all ─────────────────────────────────────────────────────────────

    @Test @Order(10)
    void getAllSponsors_returnsBothCreated() throws Exception {
        mockMvc.perform(get("/api/sponsors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ── validation ───────────────────────────────────────────────────────────

    @Test @Order(11)
    void createSponsor_missingName_returns400() throws Exception {
        var bad = SponsorRequest.builder().tier(SponsorTier.SILVER).build();
        mockMvc.perform(post("/api/sponsors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }
}

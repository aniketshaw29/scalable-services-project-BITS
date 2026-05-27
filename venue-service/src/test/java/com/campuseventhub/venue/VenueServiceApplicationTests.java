package com.campuseventhub.venue;

import com.campuseventhub.venue.dto.BookingRequest;
import com.campuseventhub.venue.dto.BookingResponse;
import com.campuseventhub.venue.dto.VenueRequest;
import com.campuseventhub.venue.dto.VenueResponse;
import com.campuseventhub.venue.entity.VenueType;
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
class VenueServiceApplicationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    static Long createdVenueId;
    static Long createdBookingId;
    static Long secondVenueId;

    static final LocalDateTime SLOT_START = LocalDateTime.of(2024, 9, 1, 9, 0);
    static final LocalDateTime SLOT_END   = LocalDateTime.of(2024, 9, 1, 17, 0);
    static final Long TEST_EVENT_ID = 42L;

    @Test @Order(1)
    void contextLoads() { }

    @Test @Order(2)
    void createVenue_returns201WithId() throws Exception {
        VenueRequest req = VenueRequest.builder()
                .name("Main Auditorium")
                .location("Block A, Ground Floor")
                .capacity(500)
                .type(VenueType.AUDITORIUM)
                .facilities("Projector, AC, Mic")
                .build();

        MvcResult result = mockMvc.perform(post("/api/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Main Auditorium"))
                .andExpect(jsonPath("$.type").value("AUDITORIUM"))
                .andExpect(jsonPath("$.capacity").value(500))
                .andReturn();

        VenueResponse resp = objectMapper.readValue(result.getResponse().getContentAsString(), VenueResponse.class);
        createdVenueId = resp.getId();
    }

    @Test @Order(3)
    void createVenue_missingName_returns400() throws Exception {
        VenueRequest req = VenueRequest.builder()
                .location("Some Location")
                .capacity(100)
                .type(VenueType.CLASSROOM)
                .build();

        mockMvc.perform(post("/api/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test @Order(4)
    void getAllVenues_returnsArray() throws Exception {
        mockMvc.perform(get("/api/venues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test @Order(5)
    void getVenueById_returnsCorrectVenue() throws Exception {
        mockMvc.perform(get("/api/venues/{id}", createdVenueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdVenueId))
                .andExpect(jsonPath("$.name").value("Main Auditorium"));
    }

    @Test @Order(6)
    void getVenueById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/venues/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("99999")));
    }

    @Test @Order(7)
    void checkAvailability_noBookings_returnsAvailable() throws Exception {
        mockMvc.perform(get("/api/venues/{id}/availability", createdVenueId)
                        .param("startTime", SLOT_START.toString())
                        .param("endTime", SLOT_END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test @Order(8)
    void bookVenue_returns201WithBookingId() throws Exception {
        BookingRequest req = BookingRequest.builder()
                .eventId(TEST_EVENT_ID)
                .startTime(SLOT_START)
                .endTime(SLOT_END)
                .build();

        MvcResult result = mockMvc.perform(post("/api/venues/{id}/book", createdVenueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").exists())
                .andExpect(jsonPath("$.venueId").value(createdVenueId))
                .andExpect(jsonPath("$.eventId").value(TEST_EVENT_ID))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andReturn();

        BookingResponse resp = objectMapper.readValue(result.getResponse().getContentAsString(), BookingResponse.class);
        createdBookingId = resp.getBookingId();
    }

    @Test @Order(9)
    void checkAvailability_afterBooking_returnsUnavailable() throws Exception {
        // Overlapping slot
        LocalDateTime overlapStart = SLOT_START.plusHours(1);
        LocalDateTime overlapEnd = SLOT_END.minusHours(1);

        mockMvc.perform(get("/api/venues/{id}/availability", createdVenueId)
                        .param("startTime", overlapStart.toString())
                        .param("endTime", overlapEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.conflict").exists())
                .andExpect(jsonPath("$.conflict.status").value("BOOKED"));
    }

    @Test @Order(10)
    void bookVenue_conflictingSlot_returns409() throws Exception {
        BookingRequest conflicting = BookingRequest.builder()
                .eventId(99L)
                .startTime(SLOT_START)
                .endTime(SLOT_END)
                .build();

        mockMvc.perform(post("/api/venues/{id}/book", createdVenueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflicting)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test @Order(11)
    void getVenueByEventId_returnsVenue() throws Exception {
        mockMvc.perform(get("/api/venues/event/{eventId}", TEST_EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdVenueId))
                .andExpect(jsonPath("$.name").value("Main Auditorium"));
    }

    @Test @Order(12)
    void cancelBooking_returns204() throws Exception {
        mockMvc.perform(delete("/api/venues/bookings/{bookingId}", createdBookingId))
                .andExpect(status().isNoContent());
    }

    @Test @Order(13)
    void checkAvailability_afterCancel_returnsAvailable() throws Exception {
        mockMvc.perform(get("/api/venues/{id}/availability", createdVenueId)
                        .param("startTime", SLOT_START.toString())
                        .param("endTime", SLOT_END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }
}

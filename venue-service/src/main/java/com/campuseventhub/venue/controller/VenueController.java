package com.campuseventhub.venue.controller;

import com.campuseventhub.venue.dto.*;
import com.campuseventhub.venue.service.VenueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
@Validated
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<VenueResponse> createVenue(@Valid @RequestBody VenueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.createVenue(request));
    }

    @GetMapping
    public ResponseEntity<List<VenueResponse>> getAllVenues() {
        return ResponseEntity.ok(venueService.getAllVenues());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueResponse> getVenueById(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        return ResponseEntity.ok(venueService.getVenueById(id));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable @Positive(message = "id must be a positive number") Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(venueService.checkAvailability(id, startTime, endTime));
    }

    @PostMapping("/{id}/book")
    public ResponseEntity<BookingResponse> bookVenue(
            @PathVariable @Positive(message = "id must be a positive number") Long id,
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.bookVenue(id, request));
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable @Positive(message = "bookingId must be a positive number") Long bookingId) {
        venueService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<VenueResponse> getVenueByEventId(
            @PathVariable @Positive(message = "eventId must be a positive number") Long eventId) {
        return ResponseEntity.ok(venueService.getVenueByEventId(eventId));
    }
}

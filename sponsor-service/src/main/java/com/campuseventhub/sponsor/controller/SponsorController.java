package com.campuseventhub.sponsor.controller;

import com.campuseventhub.sponsor.dto.*;
import com.campuseventhub.sponsor.service.SponsorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sponsors")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;

    @PostMapping
    public ResponseEntity<SponsorResponse> create(@Valid @RequestBody SponsorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sponsorService.createSponsor(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SponsorResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sponsorService.getSponsorById(id));
    }

    @GetMapping
    public ResponseEntity<List<SponsorResponse>> getAll() {
        return ResponseEntity.ok(sponsorService.getAllSponsors());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SponsorResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody SponsorRequest request) {
        return ResponseEntity.ok(sponsorService.updateSponsor(id, request));
    }

    @PostMapping("/{id}/events/{eventId}")
    public ResponseEntity<EventSponsorResponse> linkToEvent(
            @PathVariable Long id,
            @PathVariable Long eventId,
            @RequestBody(required = false) LinkSponsorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sponsorService.linkSponsorToEvent(id, eventId, request));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<EventSponsorResponse>> getByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(sponsorService.getSponsorsByEvent(eventId));
    }
}

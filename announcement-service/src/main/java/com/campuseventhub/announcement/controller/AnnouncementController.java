package com.campuseventhub.announcement.controller;

import com.campuseventhub.announcement.dto.AnnouncementRequest;
import com.campuseventhub.announcement.dto.AnnouncementResponse;
import com.campuseventhub.announcement.entity.AnnouncementType;
import com.campuseventhub.announcement.service.AnnouncementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Validated
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    public ResponseEntity<AnnouncementResponse> create(@Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.createAnnouncement(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> getById(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        return ResponseEntity.ok(announcementService.getAnnouncementById(id));
    }

    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> getAll() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<AnnouncementResponse>> getByEvent(
            @PathVariable @Positive(message = "eventId must be a positive number") Long eventId) {
        return ResponseEntity.ok(announcementService.getAnnouncementsByEvent(eventId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<AnnouncementResponse>> getByType(@PathVariable AnnouncementType type) {
        return ResponseEntity.ok(announcementService.getAnnouncementsByType(type));
    }
}

package com.campuseventhub.event.controller;

import com.campuseventhub.event.dto.*;
import com.campuseventhub.event.entity.EventStatus;
import com.campuseventhub.event.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Validated
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EventResponse>> getEventsByStatus(@PathVariable EventStatus status) {
        return ResponseEntity.ok(eventService.getEventsByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable @Positive(message = "id must be a positive number") Long id,
            @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/capacity")
    public ResponseEntity<Map<String, Integer>> updateCapacity(
            @PathVariable @Positive(message = "id must be a positive number") Long id,
            @Valid @RequestBody CapacityUpdateRequest request) {
        return ResponseEntity.ok(eventService.updateCapacity(id, request));
    }
}

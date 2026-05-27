package com.campuseventhub.event.service;

import com.campuseventhub.event.dto.*;
import com.campuseventhub.event.entity.Event;
import com.campuseventhub.event.entity.EventStatus;
import com.campuseventhub.event.exception.EventCapacityFullException;
import com.campuseventhub.event.exception.EventNotFoundException;
import com.campuseventhub.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .endDate(request.getEndDate())
                .category(request.getCategory())
                .maxCapacity(request.getMaxCapacity())
                .currentRegistrations(0)
                .status(EventStatus.UPCOMING)
                .venueId(request.getVenueId())
                .build();
        return toResponse(eventRepository.save(event));
    }

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EventResponse getEventById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<EventResponse> getEventsByStatus(EventStatus status) {
        return eventRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = findOrThrow(id);
        if (request.getTitle() != null)       event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null)   event.setEventDate(request.getEventDate());
        if (request.getEndDate() != null)     event.setEndDate(request.getEndDate());
        if (request.getCategory() != null)    event.setCategory(request.getCategory());
        if (request.getMaxCapacity() > 0)     event.setMaxCapacity(request.getMaxCapacity());
        if (request.getVenueId() != null)     event.setVenueId(request.getVenueId());
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(Long id) {
        findOrThrow(id);
        eventRepository.deleteById(id);
    }

    @Transactional
    public Map<String, Integer> updateCapacity(Long id, CapacityUpdateRequest request) {
        Event event = findOrThrow(id);
        if (request.getDelta() > 0 && event.getCurrentRegistrations() >= event.getMaxCapacity()) {
            throw new EventCapacityFullException(id);
        }
        int updated = eventRepository.updateRegistrationCount(id, request.getDelta());
        if (updated == 0) {
            throw new EventCapacityFullException(id);
        }
        // Re-fetch to get updated value
        int newCount = findOrThrow(id).getCurrentRegistrations();
        return Map.of("currentRegistrations", newCount);
    }

    private Event findOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .endDate(event.getEndDate())
                .category(event.getCategory())
                .maxCapacity(event.getMaxCapacity())
                .currentRegistrations(event.getCurrentRegistrations())
                .status(event.getStatus())
                .venueId(event.getVenueId())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}

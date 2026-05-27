package com.campuseventhub.announcement.service;

import com.campuseventhub.announcement.dto.AnnouncementRequest;
import com.campuseventhub.announcement.dto.AnnouncementResponse;
import com.campuseventhub.announcement.entity.Announcement;
import com.campuseventhub.announcement.entity.AnnouncementType;
import com.campuseventhub.announcement.exception.AnnouncementNotFoundException;
import com.campuseventhub.announcement.messaging.AnnouncementCreatedEvent;
import com.campuseventhub.announcement.messaging.AnnouncementEventPublisher;
import com.campuseventhub.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementEventPublisher eventPublisher;

    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .eventId(request.getEventId())
                .type(request.getType() != null ? request.getType() : AnnouncementType.GENERAL)
                .publishedBy(request.getPublishedBy())
                .build();

        announcement = announcementRepository.save(announcement);

        eventPublisher.publishAnnouncementCreated(AnnouncementCreatedEvent.builder()
                .announcementId(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .eventId(announcement.getEventId())
                .type(announcement.getType().name())
                .publishedBy(announcement.getPublishedBy())
                .publishedAt(announcement.getPublishedAt())
                .build());

        return toResponse(announcement);
    }

    public AnnouncementResponse getAnnouncementById(Long id) {
        return announcementRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new AnnouncementNotFoundException("Announcement not found: " + id));
    }

    public List<AnnouncementResponse> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByPublishedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AnnouncementResponse> getAnnouncementsByEvent(Long eventId) {
        return announcementRepository.findByEventIdOrderByPublishedAtDesc(eventId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AnnouncementResponse> getAnnouncementsByType(AnnouncementType type) {
        return announcementRepository.findByTypeOrderByPublishedAtDesc(type)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private AnnouncementResponse toResponse(Announcement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .eventId(a.getEventId())
                .type(a.getType())
                .publishedBy(a.getPublishedBy())
                .publishedAt(a.getPublishedAt())
                .build();
    }
}

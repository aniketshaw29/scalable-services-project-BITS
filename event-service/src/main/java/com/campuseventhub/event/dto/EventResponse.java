package com.campuseventhub.event.dto;

import com.campuseventhub.event.entity.EventStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private LocalDateTime endDate;
    private String category;
    private int maxCapacity;
    private int currentRegistrations;
    private EventStatus status;
    private Long venueId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

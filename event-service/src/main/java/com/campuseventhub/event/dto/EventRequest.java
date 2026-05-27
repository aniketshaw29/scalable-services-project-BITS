package com.campuseventhub.event.dto;

import com.campuseventhub.event.entity.EventStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Event date is required")
    private LocalDateTime eventDate;

    private LocalDateTime endDate;

    private String category;

    @Min(value = 1, message = "Max capacity must be at least 1")
    private int maxCapacity = 100;

    private Long venueId;
}

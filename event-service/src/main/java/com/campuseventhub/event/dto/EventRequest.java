package com.campuseventhub.event.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventRequest {

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "eventDate is required")
    private LocalDateTime eventDate;

    private LocalDateTime endDate;

    @Size(max = 100, message = "category must not exceed 100 characters")
    private String category;

    @Min(value = 1, message = "maxCapacity must be at least 1")
    @Max(value = 100000, message = "maxCapacity must not exceed 100000")
    @Builder.Default
    private int maxCapacity = 100;

    @Positive(message = "venueId must be a positive number")
    private Long venueId;
}

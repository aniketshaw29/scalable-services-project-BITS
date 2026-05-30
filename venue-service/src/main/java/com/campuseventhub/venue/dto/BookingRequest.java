package com.campuseventhub.venue.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingRequest {

    @NotNull(message = "eventId is required")
    @Positive(message = "eventId must be a positive number")
    private Long eventId;

    @NotNull(message = "startTime is required")
    private LocalDateTime startTime;

    @NotNull(message = "endTime is required")
    private LocalDateTime endTime;
}

package com.campuseventhub.venue.dto;

import com.campuseventhub.venue.entity.BookingStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingResponse {
    private Long bookingId;
    private Long venueId;
    private Long eventId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
}

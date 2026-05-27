package com.campuseventhub.venue.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvailabilityResponse {
    private boolean available;
    private BookingResponse conflict;
}

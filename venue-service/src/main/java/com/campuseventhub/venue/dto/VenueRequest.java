package com.campuseventhub.venue.dto;

import com.campuseventhub.venue.entity.VenueType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VenueRequest {

    @NotBlank(message = "Venue name is required")
    private String name;

    private String location;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity;

    private VenueType type;

    private String facilities;
}

package com.campuseventhub.venue.dto;

import com.campuseventhub.venue.entity.VenueType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VenueRequest {

    @NotBlank(message = "name is required")
    @Size(max = 200, message = "name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "location is required")
    @Size(max = 500, message = "location must not exceed 500 characters")
    private String location;

    @Min(value = 1, message = "capacity must be at least 1")
    @Max(value = 100000, message = "capacity must not exceed 100000")
    private int capacity;

    @NotNull(message = "type is required")
    private VenueType type;

    @Size(max = 1000, message = "facilities must not exceed 1000 characters")
    private String facilities;
}

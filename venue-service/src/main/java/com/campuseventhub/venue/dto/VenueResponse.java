package com.campuseventhub.venue.dto;

import com.campuseventhub.venue.entity.VenueType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VenueResponse {
    private Long id;
    private String name;
    private String location;
    private int capacity;
    private VenueType type;
    private String facilities;
    private LocalDateTime createdAt;
}

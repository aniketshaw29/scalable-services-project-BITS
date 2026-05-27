package com.campuseventhub.venue.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "venues")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Venue name is required")
    @Column(nullable = false)
    private String name;

    private String location;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    private VenueType type;

    @Column(columnDefinition = "TEXT")
    private String facilities;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

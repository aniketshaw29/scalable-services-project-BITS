package com.campuseventhub.registration.client;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventDto {
    private Long id;
    private String title;
    private int maxCapacity;
    private int currentRegistrations;
    private String status;
    private LocalDateTime eventDate;
}

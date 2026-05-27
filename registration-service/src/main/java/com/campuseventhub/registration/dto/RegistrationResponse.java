package com.campuseventhub.registration.dto;

import com.campuseventhub.registration.entity.RegistrationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationResponse {
    private Long id;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private Long eventId;
    private LocalDateTime registeredAt;
    private RegistrationStatus status;
}

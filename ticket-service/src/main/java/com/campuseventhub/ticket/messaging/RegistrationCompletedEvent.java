package com.campuseventhub.ticket.messaging;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationCompletedEvent {
    private Long registrationId;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private Long eventId;
    private String eventTitle;
    private LocalDateTime timestamp;
}

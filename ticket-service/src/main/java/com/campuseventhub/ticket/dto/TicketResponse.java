package com.campuseventhub.ticket.dto;

import com.campuseventhub.ticket.entity.TicketStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketResponse {
    private Long id;
    private Long registrationId;
    private String studentId;
    private Long eventId;
    private String qrCode;
    private LocalDateTime generatedAt;
    private TicketStatus status;
}

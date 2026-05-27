package com.campuseventhub.ticket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_id", nullable = false, unique = true)
    private Long registrationId;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @CreationTimestamp
    private LocalDateTime generatedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TicketStatus status = TicketStatus.VALID;
}

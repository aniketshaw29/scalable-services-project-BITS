package com.campuseventhub.sponsor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_sponsors",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "sponsor_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventSponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id", nullable = false)
    private Sponsor sponsor;

    @Column(precision = 12, scale = 2)
    private BigDecimal contribution;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    private LocalDateTime linkedAt;
}

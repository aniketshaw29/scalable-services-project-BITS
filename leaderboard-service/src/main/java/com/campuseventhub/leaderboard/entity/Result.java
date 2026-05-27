package com.campuseventhub.leaderboard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "event_title")
    private String eventTitle;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "student_name")
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    private String category;

    @Builder.Default
    private Integer points = 0;

    @CreationTimestamp
    private LocalDateTime publishedAt;
}

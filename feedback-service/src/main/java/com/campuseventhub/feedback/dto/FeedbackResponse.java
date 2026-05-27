package com.campuseventhub.feedback.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedbackResponse {
    private Long id;
    private String studentId;
    private String studentName;
    private Long eventId;
    private Integer rating;
    private String comment;
    private LocalDateTime submittedAt;
}

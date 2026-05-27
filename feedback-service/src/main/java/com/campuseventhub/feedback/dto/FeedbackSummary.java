package com.campuseventhub.feedback.dto;

import lombok.*;

import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedbackSummary {
    private Long eventId;
    private double averageRating;
    private long totalResponses;
    private Map<Integer, Long> ratingDistribution;
}

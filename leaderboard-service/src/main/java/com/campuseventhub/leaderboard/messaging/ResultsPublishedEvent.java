package com.campuseventhub.leaderboard.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultsPublishedEvent {
    private Long eventId;
    private String eventTitle;
    private List<ResultEntry> results;
    private LocalDateTime publishedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultEntry {
        private String studentId;
        private String studentName;
        private String position;
        private Integer points;
    }
}

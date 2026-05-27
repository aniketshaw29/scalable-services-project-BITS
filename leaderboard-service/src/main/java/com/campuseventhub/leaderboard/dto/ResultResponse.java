package com.campuseventhub.leaderboard.dto;

import com.campuseventhub.leaderboard.entity.Position;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResultResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String studentId;
    private String studentName;
    private Position position;
    private String category;
    private Integer points;
    private LocalDateTime publishedAt;
}

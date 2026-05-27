package com.campuseventhub.leaderboard.dto;

import com.campuseventhub.leaderboard.entity.Position;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResultRequest {

    @NotNull(message = "eventId is required")
    private Long eventId;

    private String eventTitle;

    @NotBlank(message = "studentId is required")
    private String studentId;

    private String studentName;

    @NotNull(message = "position is required")
    private Position position;

    private String category;

    private Integer points;
}

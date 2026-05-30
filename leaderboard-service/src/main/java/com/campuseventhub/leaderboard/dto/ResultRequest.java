package com.campuseventhub.leaderboard.dto;

import com.campuseventhub.leaderboard.entity.Position;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResultRequest {

    @NotNull(message = "eventId is required")
    @Positive(message = "eventId must be a positive number")
    private Long eventId;

    @Size(max = 200, message = "eventTitle must not exceed 200 characters")
    private String eventTitle;

    @NotBlank(message = "studentId is required")
    @Size(max = 50, message = "studentId must not exceed 50 characters")
    private String studentId;

    @Size(max = 200, message = "studentName must not exceed 200 characters")
    private String studentName;

    @NotNull(message = "position is required")
    private Position position;

    @Size(max = 100, message = "category must not exceed 100 characters")
    private String category;

    @Positive(message = "points must be a positive number")
    private Integer points;
}

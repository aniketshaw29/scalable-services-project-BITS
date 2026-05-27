package com.campuseventhub.feedback.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedbackRequest {

    @NotBlank(message = "studentId is required")
    private String studentId;

    private String studentName;

    @NotNull(message = "eventId is required")
    private Long eventId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be at least 1")
    @Max(value = 5, message = "rating must be at most 5")
    private Integer rating;

    @Size(max = 1000, message = "comment must not exceed 1000 characters")
    private String comment;
}

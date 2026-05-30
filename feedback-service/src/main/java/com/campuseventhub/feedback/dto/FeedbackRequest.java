package com.campuseventhub.feedback.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedbackRequest {

    @NotBlank(message = "studentId is required")
    @Size(max = 50, message = "studentId must not exceed 50 characters")
    private String studentId;

    @Size(max = 200, message = "studentName must not exceed 200 characters")
    private String studentName;

    @NotNull(message = "eventId is required")
    @Positive(message = "eventId must be a positive number")
    private Long eventId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be at least 1")
    @Max(value = 5, message = "rating must be at most 5")
    private Integer rating;

    @Size(max = 1000, message = "comment must not exceed 1000 characters")
    private String comment;
}

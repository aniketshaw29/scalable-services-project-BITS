package com.campuseventhub.attendance.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceRequest {

    @NotNull(message = "registrationId is required")
    @Positive(message = "registrationId must be a positive number")
    private Long registrationId;

    @NotBlank(message = "studentId is required")
    @Size(max = 50, message = "studentId must not exceed 50 characters")
    private String studentId;

    @NotNull(message = "eventId is required")
    @Positive(message = "eventId must be a positive number")
    private Long eventId;

    @Size(max = 200, message = "studentName must not exceed 200 characters")
    private String studentName;

    @Email(message = "studentEmail must be a valid email address")
    @Size(max = 300, message = "studentEmail must not exceed 300 characters")
    private String studentEmail;

    @Size(max = 200, message = "eventTitle must not exceed 200 characters")
    private String eventTitle;
}

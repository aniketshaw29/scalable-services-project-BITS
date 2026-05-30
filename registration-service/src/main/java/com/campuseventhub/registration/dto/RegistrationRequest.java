package com.campuseventhub.registration.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationRequest {

    @NotBlank(message = "studentId is required")
    @Size(max = 50, message = "studentId must not exceed 50 characters")
    private String studentId;

    @NotBlank(message = "studentName is required")
    @Size(max = 200, message = "studentName must not exceed 200 characters")
    private String studentName;

    @NotBlank(message = "studentEmail is required")
    @Email(message = "studentEmail must be a valid email address")
    @Size(max = 300, message = "studentEmail must not exceed 300 characters")
    private String studentEmail;

    @NotNull(message = "eventId is required")
    @Positive(message = "eventId must be a positive number")
    private Long eventId;
}

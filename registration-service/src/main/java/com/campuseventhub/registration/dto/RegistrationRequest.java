package com.campuseventhub.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationRequest {

    @NotBlank(message = "studentId is required")
    private String studentId;

    @NotBlank(message = "studentName is required")
    private String studentName;

    @NotBlank(message = "studentEmail is required")
    @Email(message = "studentEmail must be a valid email")
    private String studentEmail;

    @NotNull(message = "eventId is required")
    private Long eventId;
}

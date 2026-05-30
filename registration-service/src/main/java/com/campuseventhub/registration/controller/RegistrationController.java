package com.campuseventhub.registration.controller;

import com.campuseventhub.registration.dto.ExistsResponse;
import com.campuseventhub.registration.dto.RegistrationRequest;
import com.campuseventhub.registration.dto.RegistrationResponse;
import com.campuseventhub.registration.service.RegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
@Validated
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegistrationResponse> getById(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        return ResponseEntity.ok(registrationService.getById(id));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<RegistrationResponse>> getByEventId(
            @PathVariable @Positive(message = "eventId must be a positive number") Long eventId) {
        return ResponseEntity.ok(registrationService.getByEventId(eventId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<RegistrationResponse>> getByStudentId(
            @PathVariable @NotBlank(message = "studentId must not be blank") String studentId) {
        return ResponseEntity.ok(registrationService.getByStudentId(studentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        registrationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<ExistsResponse> checkExists(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        return ResponseEntity.ok(registrationService.checkExists(id));
    }
}

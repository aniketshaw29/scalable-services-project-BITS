package com.campuseventhub.registration.controller;

import com.campuseventhub.registration.dto.ExistsResponse;
import com.campuseventhub.registration.dto.RegistrationRequest;
import com.campuseventhub.registration.dto.RegistrationResponse;
import com.campuseventhub.registration.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegistrationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.getById(id));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<RegistrationResponse>> getByEventId(@PathVariable Long eventId) {
        return ResponseEntity.ok(registrationService.getByEventId(eventId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<RegistrationResponse>> getByStudentId(@PathVariable String studentId) {
        return ResponseEntity.ok(registrationService.getByStudentId(studentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        registrationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<ExistsResponse> checkExists(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.checkExists(id));
    }
}

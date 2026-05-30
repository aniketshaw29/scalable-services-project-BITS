package com.campuseventhub.attendance.controller;

import com.campuseventhub.attendance.dto.AttendanceRequest;
import com.campuseventhub.attendance.dto.AttendanceResponse;
import com.campuseventhub.attendance.dto.AttendanceStatusResponse;
import com.campuseventhub.attendance.service.AttendanceService;
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
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Validated
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    public ResponseEntity<AttendanceResponse> markAttendance(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.markAttendance(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceResponse> getById(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        return ResponseEntity.ok(attendanceService.getAttendanceById(id));
    }

    @GetMapping("/{registrationId}/status")
    public ResponseEntity<AttendanceStatusResponse> getStatus(
            @PathVariable @Positive(message = "registrationId must be a positive number") Long registrationId) {
        return ResponseEntity.ok(attendanceService.getStatusByRegistrationId(registrationId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<AttendanceResponse>> getByEvent(
            @PathVariable @Positive(message = "eventId must be a positive number") Long eventId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEvent(eventId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<AttendanceResponse>> getByStudent(
            @PathVariable @NotBlank(message = "studentId must not be blank") String studentId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByStudent(studentId));
    }
}

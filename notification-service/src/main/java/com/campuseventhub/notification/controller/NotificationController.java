package com.campuseventhub.notification.controller;

import com.campuseventhub.notification.dto.NotificationResponse;
import com.campuseventhub.notification.entity.NotificationType;
import com.campuseventhub.notification.service.NotificationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        return ResponseEntity.ok(notificationService.getById(id));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<NotificationResponse>> getByStudentId(
            @PathVariable @NotBlank(message = "studentId must not be blank") String studentId) {
        return ResponseEntity.ok(notificationService.getByStudentId(studentId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<NotificationResponse>> getByType(@PathVariable NotificationType type) {
        return ResponseEntity.ok(notificationService.getByType(type));
    }
}

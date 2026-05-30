package com.campuseventhub.feedback.controller;

import com.campuseventhub.feedback.dto.FeedbackRequest;
import com.campuseventhub.feedback.dto.FeedbackResponse;
import com.campuseventhub.feedback.dto.FeedbackSummary;
import com.campuseventhub.feedback.service.FeedbackService;
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
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Validated
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackResponse> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.submitFeedback(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponse> getById(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<FeedbackResponse>> getByEvent(
            @PathVariable @Positive(message = "eventId must be a positive number") Long eventId) {
        return ResponseEntity.ok(feedbackService.getFeedbackByEvent(eventId));
    }

    @GetMapping("/event/{eventId}/summary")
    public ResponseEntity<FeedbackSummary> getSummary(
            @PathVariable @Positive(message = "eventId must be a positive number") Long eventId) {
        return ResponseEntity.ok(feedbackService.getSummaryByEvent(eventId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<FeedbackResponse>> getByStudent(
            @PathVariable @NotBlank(message = "studentId must not be blank") String studentId) {
        return ResponseEntity.ok(feedbackService.getFeedbackByStudent(studentId));
    }
}

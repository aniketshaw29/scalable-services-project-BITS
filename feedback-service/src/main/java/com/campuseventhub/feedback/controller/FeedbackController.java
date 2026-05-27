package com.campuseventhub.feedback.controller;

import com.campuseventhub.feedback.dto.FeedbackRequest;
import com.campuseventhub.feedback.dto.FeedbackResponse;
import com.campuseventhub.feedback.dto.FeedbackSummary;
import com.campuseventhub.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackResponse> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.submitFeedback(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<FeedbackResponse>> getByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(feedbackService.getFeedbackByEvent(eventId));
    }

    @GetMapping("/event/{eventId}/summary")
    public ResponseEntity<FeedbackSummary> getSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(feedbackService.getSummaryByEvent(eventId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<FeedbackResponse>> getByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(feedbackService.getFeedbackByStudent(studentId));
    }
}

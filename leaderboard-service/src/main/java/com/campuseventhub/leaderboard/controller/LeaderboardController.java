package com.campuseventhub.leaderboard.controller;

import com.campuseventhub.leaderboard.dto.ResultRequest;
import com.campuseventhub.leaderboard.dto.ResultResponse;
import com.campuseventhub.leaderboard.service.LeaderboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @PostMapping("/results")
    public ResponseEntity<ResultResponse> publishResult(@Valid @RequestBody ResultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaderboardService.publishResult(request));
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<ResultResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leaderboardService.getResultById(id));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<ResultResponse>> getByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(leaderboardService.getResultsByEvent(eventId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ResultResponse>> getByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(leaderboardService.getResultsByStudent(studentId));
    }

    @GetMapping("/top")
    public ResponseEntity<List<ResultResponse>> getTopPerformers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(leaderboardService.getTopPerformers(limit));
    }
}

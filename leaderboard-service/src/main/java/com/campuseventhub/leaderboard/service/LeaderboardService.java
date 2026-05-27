package com.campuseventhub.leaderboard.service;

import com.campuseventhub.leaderboard.dto.ResultRequest;
import com.campuseventhub.leaderboard.dto.ResultResponse;
import com.campuseventhub.leaderboard.entity.Position;
import com.campuseventhub.leaderboard.entity.Result;
import com.campuseventhub.leaderboard.exception.ResultNotFoundException;
import com.campuseventhub.leaderboard.messaging.ResultsEventPublisher;
import com.campuseventhub.leaderboard.messaging.ResultsPublishedEvent;
import com.campuseventhub.leaderboard.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final ResultRepository resultRepository;
    private final ResultsEventPublisher eventPublisher;

    @Transactional
    public ResultResponse publishResult(ResultRequest request) {
        Result result = Result.builder()
                .eventId(request.getEventId())
                .eventTitle(request.getEventTitle())
                .studentId(request.getStudentId())
                .studentName(request.getStudentName())
                .position(request.getPosition())
                .category(request.getCategory())
                .points(request.getPoints() != null ? request.getPoints() : pointsForPosition(request.getPosition()))
                .build();

        result = resultRepository.save(result);

        eventPublisher.publishResults(ResultsPublishedEvent.builder()
                .eventId(result.getEventId())
                .eventTitle(result.getEventTitle())
                .publishedAt(LocalDateTime.now())
                .results(List.of(ResultsPublishedEvent.ResultEntry.builder()
                        .studentId(result.getStudentId())
                        .studentName(result.getStudentName())
                        .position(result.getPosition().name())
                        .points(result.getPoints())
                        .build()))
                .build());

        return toResponse(result);
    }

    public ResultResponse getResultById(Long id) {
        return resultRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResultNotFoundException("Result not found: " + id));
    }

    public List<ResultResponse> getResultsByEvent(Long eventId) {
        return resultRepository.findByEventIdOrderByPointsDesc(eventId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ResultResponse> getResultsByStudent(String studentId) {
        return resultRepository.findByStudentIdOrderByPublishedAtDesc(studentId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ResultResponse> getTopPerformers(int limit) {
        return resultRepository.findTopPerformers()
                .stream().limit(limit).map(this::toResponse).collect(Collectors.toList());
    }

    private int pointsForPosition(Position position) {
        return switch (position) {
            case FIRST -> 100;
            case SECOND -> 75;
            case THIRD -> 50;
            case PARTICIPANT -> 10;
        };
    }

    private ResultResponse toResponse(Result r) {
        return ResultResponse.builder()
                .id(r.getId())
                .eventId(r.getEventId())
                .eventTitle(r.getEventTitle())
                .studentId(r.getStudentId())
                .studentName(r.getStudentName())
                .position(r.getPosition())
                .category(r.getCategory())
                .points(r.getPoints())
                .publishedAt(r.getPublishedAt())
                .build();
    }
}

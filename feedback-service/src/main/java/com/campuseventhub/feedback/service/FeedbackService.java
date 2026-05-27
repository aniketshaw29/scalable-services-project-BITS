package com.campuseventhub.feedback.service;

import com.campuseventhub.feedback.dto.FeedbackRequest;
import com.campuseventhub.feedback.dto.FeedbackResponse;
import com.campuseventhub.feedback.dto.FeedbackSummary;
import com.campuseventhub.feedback.entity.Feedback;
import com.campuseventhub.feedback.exception.FeedbackAlreadySubmittedException;
import com.campuseventhub.feedback.exception.FeedbackNotFoundException;
import com.campuseventhub.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public FeedbackResponse submitFeedback(FeedbackRequest request) {
        if (feedbackRepository.existsByStudentIdAndEventId(request.getStudentId(), request.getEventId())) {
            throw new FeedbackAlreadySubmittedException(request.getStudentId(), request.getEventId());
        }

        Feedback feedback = Feedback.builder()
                .studentId(request.getStudentId())
                .studentName(request.getStudentName())
                .eventId(request.getEventId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return toResponse(feedbackRepository.save(feedback));
    }

    public FeedbackResponse getFeedbackById(Long id) {
        return feedbackRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new FeedbackNotFoundException("Feedback not found: " + id));
    }

    public List<FeedbackResponse> getFeedbackByEvent(Long eventId) {
        return feedbackRepository.findByEventId(eventId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<FeedbackResponse> getFeedbackByStudent(String studentId) {
        return feedbackRepository.findByStudentId(studentId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public FeedbackSummary getSummaryByEvent(Long eventId) {
        List<Feedback> feedbacks = feedbackRepository.findByEventId(eventId);

        if (feedbacks.isEmpty()) {
            return FeedbackSummary.builder()
                    .eventId(eventId)
                    .averageRating(0.0)
                    .totalResponses(0)
                    .ratingDistribution(Map.of())
                    .build();
        }

        double avg = feedbacks.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
        Map<Integer, Long> dist = feedbacks.stream()
                .collect(Collectors.groupingBy(Feedback::getRating, Collectors.counting()));

        return FeedbackSummary.builder()
                .eventId(eventId)
                .averageRating(Math.round(avg * 10.0) / 10.0)
                .totalResponses(feedbacks.size())
                .ratingDistribution(dist)
                .build();
    }

    private FeedbackResponse toResponse(Feedback f) {
        return FeedbackResponse.builder()
                .id(f.getId())
                .studentId(f.getStudentId())
                .studentName(f.getStudentName())
                .eventId(f.getEventId())
                .rating(f.getRating())
                .comment(f.getComment())
                .submittedAt(f.getSubmittedAt())
                .build();
    }
}

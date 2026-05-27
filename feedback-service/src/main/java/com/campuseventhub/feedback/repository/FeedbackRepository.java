package com.campuseventhub.feedback.repository;

import com.campuseventhub.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByEventId(Long eventId);
    List<Feedback> findByStudentId(String studentId);
    Optional<Feedback> findByStudentIdAndEventId(String studentId, Long eventId);
    boolean existsByStudentIdAndEventId(String studentId, Long eventId);
}

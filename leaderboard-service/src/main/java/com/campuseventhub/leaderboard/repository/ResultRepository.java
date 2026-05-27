package com.campuseventhub.leaderboard.repository;

import com.campuseventhub.leaderboard.entity.Position;
import com.campuseventhub.leaderboard.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findByEventIdOrderByPointsDesc(Long eventId);
    List<Result> findByStudentIdOrderByPublishedAtDesc(String studentId);
    List<Result> findByPositionOrderByPointsDesc(Position position);

    @Query("SELECT r FROM Result r ORDER BY r.points DESC")
    List<Result> findTopPerformers();
}

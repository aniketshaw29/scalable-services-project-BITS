package com.campuseventhub.announcement.repository;

import com.campuseventhub.announcement.entity.Announcement;
import com.campuseventhub.announcement.entity.AnnouncementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByOrderByPublishedAtDesc();
    List<Announcement> findByEventIdOrderByPublishedAtDesc(Long eventId);
    List<Announcement> findByTypeOrderByPublishedAtDesc(AnnouncementType type);
}

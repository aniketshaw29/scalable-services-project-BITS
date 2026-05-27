package com.campuseventhub.sponsor.repository;

import com.campuseventhub.sponsor.entity.EventSponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventSponsorRepository extends JpaRepository<EventSponsor, Long> {
    List<EventSponsor> findByEventId(Long eventId);
    Optional<EventSponsor> findByEventIdAndSponsorId(Long eventId, Long sponsorId);
    boolean existsByEventIdAndSponsorId(Long eventId, Long sponsorId);
}

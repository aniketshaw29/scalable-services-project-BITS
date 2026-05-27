package com.campuseventhub.venue.repository;

import com.campuseventhub.venue.entity.BookingStatus;
import com.campuseventhub.venue.entity.VenueBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VenueBookingRepository extends JpaRepository<VenueBooking, Long> {

    Optional<VenueBooking> findByEventIdAndStatus(Long eventId, BookingStatus status);

    // Find any active booking that overlaps the requested window
    @Query("""
        SELECT b FROM VenueBooking b
        WHERE b.venueId = :venueId
          AND b.status = 'BOOKED'
          AND b.startTime < :endTime
          AND b.endTime > :startTime
    """)
    List<VenueBooking> findConflicts(Long venueId, LocalDateTime startTime, LocalDateTime endTime);
}

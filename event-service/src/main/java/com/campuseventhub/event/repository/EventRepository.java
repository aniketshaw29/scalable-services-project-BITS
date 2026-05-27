package com.campuseventhub.event.repository;

import com.campuseventhub.event.entity.Event;
import com.campuseventhub.event.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatus(EventStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Event e SET e.currentRegistrations = e.currentRegistrations + :delta WHERE e.id = :id AND e.currentRegistrations + :delta >= 0")
    int updateRegistrationCount(Long id, int delta);
}

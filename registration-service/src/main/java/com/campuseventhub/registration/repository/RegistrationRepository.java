package com.campuseventhub.registration.repository;

import com.campuseventhub.registration.entity.Registration;
import com.campuseventhub.registration.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByEventId(Long eventId);

    List<Registration> findByStudentId(String studentId);

    Optional<Registration> findByStudentIdAndEventId(String studentId, Long eventId);

    boolean existsByStudentIdAndEventIdAndStatus(String studentId, Long eventId, RegistrationStatus status);

    List<Registration> findByEventIdAndStatus(Long eventId, RegistrationStatus status);
}

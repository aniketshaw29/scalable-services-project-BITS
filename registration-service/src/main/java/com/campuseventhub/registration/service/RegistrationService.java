package com.campuseventhub.registration.service;

import com.campuseventhub.registration.client.EventClient;
import com.campuseventhub.registration.client.EventDto;
import com.campuseventhub.registration.dto.ExistsResponse;
import com.campuseventhub.registration.dto.RegistrationRequest;
import com.campuseventhub.registration.dto.RegistrationResponse;
import com.campuseventhub.registration.entity.Registration;
import com.campuseventhub.registration.entity.RegistrationStatus;
import com.campuseventhub.registration.exception.DuplicateRegistrationException;
import com.campuseventhub.registration.exception.EventCapacityFullException;
import com.campuseventhub.registration.exception.RegistrationNotFoundException;
import com.campuseventhub.registration.repository.RegistrationRepository;
import com.campuseventhub.registration.messaging.RegistrationCompletedEvent;
import com.campuseventhub.registration.messaging.RegistrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventClient eventClient;
    private final RegistrationEventPublisher eventPublisher;

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        // Check for duplicate active registration
        if (registrationRepository.existsByStudentIdAndEventIdAndStatus(
                request.getStudentId(), request.getEventId(), RegistrationStatus.ACTIVE)) {
            throw new DuplicateRegistrationException(request.getStudentId(), request.getEventId());
        }

        // Validate event exists and has capacity (Feign call — may trigger circuit breaker fallback)
        EventDto event = eventClient.getEventById(request.getEventId());
        if (event.getCurrentRegistrations() >= event.getMaxCapacity()) {
            throw new EventCapacityFullException(request.getEventId());
        }

        Registration registration = Registration.builder()
                .studentId(request.getStudentId())
                .studentName(request.getStudentName())
                .studentEmail(request.getStudentEmail())
                .eventId(request.getEventId())
                .build();
        Registration saved = registrationRepository.save(registration);

        // Increment event capacity
        eventClient.updateCapacity(request.getEventId(), Map.of("delta", 1));

        // Publish async event for Ticket + Notification services
        eventPublisher.publishRegistrationCompleted(
                RegistrationCompletedEvent.builder()
                        .registrationId(saved.getId())
                        .studentId(saved.getStudentId())
                        .studentName(saved.getStudentName())
                        .studentEmail(saved.getStudentEmail())
                        .eventId(saved.getEventId())
                        .eventTitle(event.getTitle())
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        return toResponse(saved);
    }

    public RegistrationResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<RegistrationResponse> getByEventId(Long eventId) {
        return registrationRepository.findByEventId(eventId).stream()
                .map(this::toResponse).toList();
    }

    public List<RegistrationResponse> getByStudentId(String studentId) {
        return registrationRepository.findByStudentId(studentId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void cancel(Long id) {
        Registration registration = findOrThrow(id);
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            return;
        }
        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        // Decrement event capacity
        eventClient.updateCapacity(registration.getEventId(), Map.of("delta", -1));
    }

    public ExistsResponse checkExists(Long id) {
        return registrationRepository.findById(id)
                .map(r -> ExistsResponse.builder()
                        .exists(true)
                        .status(r.getStatus().name())
                        .build())
                .orElse(ExistsResponse.builder().exists(false).build());
    }

    private Registration findOrThrow(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new RegistrationNotFoundException(id));
    }

    private RegistrationResponse toResponse(Registration r) {
        return RegistrationResponse.builder()
                .id(r.getId())
                .studentId(r.getStudentId())
                .studentName(r.getStudentName())
                .studentEmail(r.getStudentEmail())
                .eventId(r.getEventId())
                .registeredAt(r.getRegisteredAt())
                .status(r.getStatus())
                .build();
    }
}

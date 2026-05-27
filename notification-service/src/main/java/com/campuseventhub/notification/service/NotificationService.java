package com.campuseventhub.notification.service;

import com.campuseventhub.notification.dto.NotificationResponse;
import com.campuseventhub.notification.entity.Notification;
import com.campuseventhub.notification.entity.NotificationStatus;
import com.campuseventhub.notification.entity.NotificationType;
import com.campuseventhub.notification.exception.NotificationNotFoundException;
import com.campuseventhub.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification save(String recipientId, String recipientEmail,
                              NotificationType type, String subject, String message) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .recipientEmail(recipientEmail)
                .type(type)
                .subject(subject)
                .message(message)
                .status(NotificationStatus.SENT)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved id={} type={} recipient={}", saved.getId(), type, recipientId);
        return saved;
    }

    public NotificationResponse getById(Long id) {
        return toResponse(notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id)));
    }

    public List<NotificationResponse> getByStudentId(String studentId) {
        return notificationRepository.findByRecipientId(studentId).stream()
                .map(this::toResponse).toList();
    }

    public List<NotificationResponse> getByType(NotificationType type) {
        return notificationRepository.findByType(type).stream()
                .map(this::toResponse).toList();
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .recipientEmail(n.getRecipientEmail())
                .type(n.getType())
                .subject(n.getSubject())
                .message(n.getMessage())
                .sentAt(n.getSentAt())
                .status(n.getStatus())
                .build();
    }
}

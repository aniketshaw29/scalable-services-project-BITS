package com.campuseventhub.notification.dto;

import com.campuseventhub.notification.entity.NotificationStatus;
import com.campuseventhub.notification.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {
    private Long id;
    private String recipientId;
    private String recipientEmail;
    private NotificationType type;
    private String subject;
    private String message;
    private LocalDateTime sentAt;
    private NotificationStatus status;
}

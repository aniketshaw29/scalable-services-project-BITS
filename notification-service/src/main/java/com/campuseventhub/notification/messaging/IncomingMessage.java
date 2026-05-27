package com.campuseventhub.notification.messaging;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncomingMessage {
    private String eventType;
    private LocalDateTime timestamp;
    // Generic payload — deserialized as Map for flexibility
    private Map<String, Object> payload;
}

package com.campuseventhub.announcement.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementCreatedEvent {
    private Long announcementId;
    private String title;
    private String content;
    private Long eventId;
    private String type;
    private String publishedBy;
    private LocalDateTime publishedAt;
}

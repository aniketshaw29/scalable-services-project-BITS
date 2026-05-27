package com.campuseventhub.announcement.dto;

import com.campuseventhub.announcement.entity.AnnouncementType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String content;
    private Long eventId;
    private AnnouncementType type;
    private String publishedBy;
    private LocalDateTime publishedAt;
}

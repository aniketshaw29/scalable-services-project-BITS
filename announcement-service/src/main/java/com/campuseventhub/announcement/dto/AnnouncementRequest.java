package com.campuseventhub.announcement.dto;

import com.campuseventhub.announcement.entity.AnnouncementType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnouncementRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "content is required")
    private String content;

    private Long eventId;

    private AnnouncementType type;

    private String publishedBy;
}

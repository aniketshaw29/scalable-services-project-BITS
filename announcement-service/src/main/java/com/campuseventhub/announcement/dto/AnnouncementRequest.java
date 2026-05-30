package com.campuseventhub.announcement.dto;

import com.campuseventhub.announcement.entity.AnnouncementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnouncementRequest {

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "content is required")
    @Size(max = 5000, message = "content must not exceed 5000 characters")
    private String content;

    @Positive(message = "eventId must be a positive number")
    private Long eventId;

    private AnnouncementType type;

    @Size(max = 100, message = "publishedBy must not exceed 100 characters")
    private String publishedBy;
}

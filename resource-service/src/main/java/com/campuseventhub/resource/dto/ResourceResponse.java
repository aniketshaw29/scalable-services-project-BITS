package com.campuseventhub.resource.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResourceResponse {
    private Long id;
    private Long eventId;
    private String uploadedBy;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String storageKey;
    private String description;
    private LocalDateTime uploadedAt;
}

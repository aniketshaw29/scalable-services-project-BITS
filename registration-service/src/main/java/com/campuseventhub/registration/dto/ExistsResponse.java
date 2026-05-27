package com.campuseventhub.registration.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExistsResponse {
    private boolean exists;
    private String status;
}

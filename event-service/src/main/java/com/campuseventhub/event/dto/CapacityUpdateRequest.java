package com.campuseventhub.event.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CapacityUpdateRequest {
    private int delta; // +1 to register, -1 to cancel
}

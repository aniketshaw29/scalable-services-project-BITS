package com.campuseventhub.sponsor.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventSponsorResponse {
    private Long id;
    private Long eventId;
    private SponsorResponse sponsor;
    private BigDecimal contribution;
    private String notes;
    private LocalDateTime linkedAt;
}

package com.campuseventhub.sponsor.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LinkSponsorRequest {
    private BigDecimal contribution;
    private String notes;
}

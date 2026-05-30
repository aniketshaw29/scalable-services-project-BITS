package com.campuseventhub.sponsor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LinkSponsorRequest {

    @DecimalMin(value = "0.0", inclusive = false, message = "contribution must be greater than 0")
    private BigDecimal contribution;

    @Size(max = 1000, message = "notes must not exceed 1000 characters")
    private String notes;
}

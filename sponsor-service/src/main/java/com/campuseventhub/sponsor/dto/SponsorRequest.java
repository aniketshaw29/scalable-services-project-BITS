package com.campuseventhub.sponsor.dto;

import com.campuseventhub.sponsor.entity.SponsorTier;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SponsorRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String logoUrl;
    private String website;
    private SponsorTier tier;
    private String contactPerson;
    private String contactEmail;
    private String description;
}

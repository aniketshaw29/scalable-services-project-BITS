package com.campuseventhub.sponsor.dto;

import com.campuseventhub.sponsor.entity.SponsorTier;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SponsorResponse {
    private Long id;
    private String name;
    private String logoUrl;
    private String website;
    private SponsorTier tier;
    private String contactPerson;
    private String contactEmail;
    private String description;
}

package com.campuseventhub.sponsor.dto;

import com.campuseventhub.sponsor.entity.SponsorTier;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SponsorRequest {

    @NotBlank(message = "name is required")
    @Size(max = 200, message = "name must not exceed 200 characters")
    private String name;

    @Size(max = 500, message = "logoUrl must not exceed 500 characters")
    private String logoUrl;

    @Size(max = 500, message = "website must not exceed 500 characters")
    private String website;

    private SponsorTier tier;

    @Size(max = 200, message = "contactPerson must not exceed 200 characters")
    private String contactPerson;

    @Email(message = "contactEmail must be a valid email address")
    @Size(max = 300, message = "contactEmail must not exceed 300 characters")
    private String contactEmail;

    @Size(max = 2000, message = "description must not exceed 2000 characters")
    private String description;
}

package com.campuseventhub.sponsor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sponsors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "logo_url")
    private String logoUrl;

    private String website;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SponsorTier tier = SponsorTier.BRONZE;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(length = 1000)
    private String description;
}

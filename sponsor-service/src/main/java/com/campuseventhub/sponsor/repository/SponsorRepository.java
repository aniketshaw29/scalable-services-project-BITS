package com.campuseventhub.sponsor.repository;

import com.campuseventhub.sponsor.entity.Sponsor;
import com.campuseventhub.sponsor.entity.SponsorTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long> {
    List<Sponsor> findByTier(SponsorTier tier);
}

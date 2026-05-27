package com.campuseventhub.sponsor.service;

import com.campuseventhub.sponsor.dto.*;
import com.campuseventhub.sponsor.entity.EventSponsor;
import com.campuseventhub.sponsor.entity.Sponsor;
import com.campuseventhub.sponsor.entity.SponsorTier;
import com.campuseventhub.sponsor.exception.SponsorAlreadyLinkedException;
import com.campuseventhub.sponsor.exception.SponsorNotFoundException;
import com.campuseventhub.sponsor.repository.EventSponsorRepository;
import com.campuseventhub.sponsor.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SponsorService {

    private final SponsorRepository sponsorRepository;
    private final EventSponsorRepository eventSponsorRepository;

    @Transactional
    public SponsorResponse createSponsor(SponsorRequest request) {
        Sponsor sponsor = Sponsor.builder()
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .website(request.getWebsite())
                .tier(request.getTier() != null ? request.getTier() : SponsorTier.BRONZE)
                .contactPerson(request.getContactPerson())
                .contactEmail(request.getContactEmail())
                .description(request.getDescription())
                .build();
        return toResponse(sponsorRepository.save(sponsor));
    }

    public SponsorResponse getSponsorById(Long id) {
        return sponsorRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new SponsorNotFoundException("Sponsor not found: " + id));
    }

    public List<SponsorResponse> getAllSponsors() {
        return sponsorRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public SponsorResponse updateSponsor(Long id, SponsorRequest request) {
        Sponsor sponsor = sponsorRepository.findById(id)
                .orElseThrow(() -> new SponsorNotFoundException("Sponsor not found: " + id));
        sponsor.setName(request.getName());
        sponsor.setLogoUrl(request.getLogoUrl());
        sponsor.setWebsite(request.getWebsite());
        if (request.getTier() != null) sponsor.setTier(request.getTier());
        sponsor.setContactPerson(request.getContactPerson());
        sponsor.setContactEmail(request.getContactEmail());
        sponsor.setDescription(request.getDescription());
        return toResponse(sponsorRepository.save(sponsor));
    }

    @Transactional
    public EventSponsorResponse linkSponsorToEvent(Long sponsorId, Long eventId, LinkSponsorRequest request) {
        if (eventSponsorRepository.existsByEventIdAndSponsorId(eventId, sponsorId)) {
            throw new SponsorAlreadyLinkedException(sponsorId, eventId);
        }
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new SponsorNotFoundException("Sponsor not found: " + sponsorId));

        EventSponsor link = EventSponsor.builder()
                .eventId(eventId)
                .sponsor(sponsor)
                .contribution(request != null ? request.getContribution() : null)
                .notes(request != null ? request.getNotes() : null)
                .build();

        return toEventSponsorResponse(eventSponsorRepository.save(link));
    }

    public List<EventSponsorResponse> getSponsorsByEvent(Long eventId) {
        return eventSponsorRepository.findByEventId(eventId)
                .stream().map(this::toEventSponsorResponse).collect(Collectors.toList());
    }

    private SponsorResponse toResponse(Sponsor s) {
        return SponsorResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .logoUrl(s.getLogoUrl())
                .website(s.getWebsite())
                .tier(s.getTier())
                .contactPerson(s.getContactPerson())
                .contactEmail(s.getContactEmail())
                .description(s.getDescription())
                .build();
    }

    private EventSponsorResponse toEventSponsorResponse(EventSponsor es) {
        return EventSponsorResponse.builder()
                .id(es.getId())
                .eventId(es.getEventId())
                .sponsor(toResponse(es.getSponsor()))
                .contribution(es.getContribution())
                .notes(es.getNotes())
                .linkedAt(es.getLinkedAt())
                .build();
    }
}

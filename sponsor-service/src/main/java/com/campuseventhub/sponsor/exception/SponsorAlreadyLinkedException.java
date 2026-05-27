package com.campuseventhub.sponsor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SponsorAlreadyLinkedException extends RuntimeException {
    public SponsorAlreadyLinkedException(Long sponsorId, Long eventId) {
        super("Sponsor " + sponsorId + " is already linked to event " + eventId);
    }
}

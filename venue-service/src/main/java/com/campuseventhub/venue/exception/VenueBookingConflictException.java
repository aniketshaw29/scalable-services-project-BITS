package com.campuseventhub.venue.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class VenueBookingConflictException extends RuntimeException {
    public VenueBookingConflictException(Long venueId) {
        super("Venue " + venueId + " is already booked for this time slot");
    }
}

package com.campuseventhub.event.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EventCapacityFullException extends RuntimeException {
    public EventCapacityFullException(Long eventId) {
        super("Event is at full capacity: " + eventId);
    }
}

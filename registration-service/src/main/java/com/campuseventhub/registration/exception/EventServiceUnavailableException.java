package com.campuseventhub.registration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class EventServiceUnavailableException extends RuntimeException {
    public EventServiceUnavailableException() {
        super("Event service unavailable. Please try again later.");
    }
}

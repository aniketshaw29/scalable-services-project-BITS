package com.campuseventhub.registration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateRegistrationException extends RuntimeException {
    public DuplicateRegistrationException(String studentId, Long eventId) {
        super("Student " + studentId + " is already registered for event " + eventId);
    }
}

package com.campuseventhub.feedback.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class FeedbackAlreadySubmittedException extends RuntimeException {
    public FeedbackAlreadySubmittedException(String studentId, Long eventId) {
        super("Feedback already submitted by student " + studentId + " for event " + eventId);
    }
}

package com.campuseventhub.sponsor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SponsorNotFoundException extends RuntimeException {
    public SponsorNotFoundException(String msg) { super(msg); }
}

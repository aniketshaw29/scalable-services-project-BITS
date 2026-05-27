package com.campuseventhub.leaderboard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResultNotFoundException extends RuntimeException {
    public ResultNotFoundException(String msg) { super(msg); }
}

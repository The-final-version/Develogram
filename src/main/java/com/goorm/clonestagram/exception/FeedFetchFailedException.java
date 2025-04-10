package com.goorm.clonestagram.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FeedFetchFailedException extends RuntimeException {

    public FeedFetchFailedException(String message) {
        super(message);
    }
}

package com.goorm.clonestagram.exception;

public class FeedFetchFailedException extends RuntimeException {
    public FeedFetchFailedException(String message) {
        super(message);
    }
}

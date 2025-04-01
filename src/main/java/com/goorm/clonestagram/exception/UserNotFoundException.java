package com.goorm.clonestagram.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("존재하지 않는 사용자입니다. ID: " + userId);
    }
}

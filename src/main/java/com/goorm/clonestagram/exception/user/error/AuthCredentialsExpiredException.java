package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.ErrorCode;

import jakarta.security.auth.message.AuthException;

public class AuthCredentialsExpiredException extends AuthException {
	public AuthCredentialsExpiredException() {
		super(String.valueOf(ErrorCode.AUTH_CREDENTIALS_EXPIRED));
	}
}

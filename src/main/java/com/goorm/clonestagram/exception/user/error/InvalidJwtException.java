package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.ErrorCode;

import jakarta.security.auth.message.AuthException;

public class InvalidJwtException extends AuthException {
	public InvalidJwtException()         { super(String.valueOf(ErrorCode.AUTH_INVALID_JWT)); }
}

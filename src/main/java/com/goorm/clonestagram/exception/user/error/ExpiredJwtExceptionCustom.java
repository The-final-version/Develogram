package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.ErrorCode;

import jakarta.security.auth.message.AuthException;

public class ExpiredJwtExceptionCustom extends AuthException {
	public ExpiredJwtExceptionCustom()   { super(String.valueOf(ErrorCode.AUTH_EXPIRED_JWT)); }
}

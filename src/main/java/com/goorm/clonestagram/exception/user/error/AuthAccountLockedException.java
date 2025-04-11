package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.ErrorCode;

import jakarta.security.auth.message.AuthException;

public class AuthAccountLockedException extends AuthException {
	public AuthAccountLockedException() {
		super(String.valueOf(ErrorCode.AUTH_ACCOUNT_LOCKED));
	}
}

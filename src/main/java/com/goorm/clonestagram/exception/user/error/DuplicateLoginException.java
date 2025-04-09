package com.goorm.clonestagram.exception.user.error;

import org.springframework.security.core.AuthenticationException;

public class DuplicateLoginException extends AuthenticationException {
	public DuplicateLoginException(String msg) {
		super(msg);
	}
}

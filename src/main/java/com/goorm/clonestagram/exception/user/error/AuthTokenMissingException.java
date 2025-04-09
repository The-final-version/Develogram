package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.BusinessException;
import com.goorm.clonestagram.exception.user.ErrorCode;

public class AuthTokenMissingException extends BusinessException {
	public AuthTokenMissingException() { super(ErrorCode.AUTH_TOKEN_MISSING); }
}

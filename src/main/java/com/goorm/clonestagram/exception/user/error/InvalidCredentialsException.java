package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.BusinessException;
import com.goorm.clonestagram.exception.user.ErrorCode;

public class InvalidCredentialsException extends BusinessException {
	public InvalidCredentialsException() { super(ErrorCode.AUTH_INVALID_CREDENTIALS); }
}

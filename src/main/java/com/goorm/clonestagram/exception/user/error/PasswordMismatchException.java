package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.BusinessException;
import com.goorm.clonestagram.exception.user.ErrorCode;

public class PasswordMismatchException extends BusinessException {
	public PasswordMismatchException() { super(ErrorCode.USER_PASSWORD_MISMATCH); }
}

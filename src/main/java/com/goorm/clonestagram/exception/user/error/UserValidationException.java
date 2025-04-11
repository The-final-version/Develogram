package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.BusinessException;
import com.goorm.clonestagram.exception.user.ErrorCode;

public class UserValidationException extends BusinessException {
	public UserValidationException(ErrorCode code) { super(code); } // USER_1xx 계열
}

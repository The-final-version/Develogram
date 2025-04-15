package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.BusinessException;
import com.goorm.clonestagram.exception.user.ErrorCode;

public class UserNotFoundException extends BusinessException {
	public UserNotFoundException() {
		super(ErrorCode.USER_NOT_FOUND);
	}



	public ErrorCode getErrorCode() {
		return ErrorCode.USER_NOT_FOUND;
	}
}

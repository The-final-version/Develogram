package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.BusinessException;
import com.goorm.clonestagram.exception.user.ErrorCode;

public class DuplicateEmailException extends BusinessException {
	public DuplicateEmailException() { super(ErrorCode.USER_DUPLICATE_EMAIL); }
}

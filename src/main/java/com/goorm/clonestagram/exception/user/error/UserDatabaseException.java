package com.goorm.clonestagram.exception.user.error;

import com.goorm.clonestagram.exception.user.BusinessException;
import com.goorm.clonestagram.exception.user.ErrorCode;

public class UserDatabaseException extends BusinessException {
	public UserDatabaseException() { super(ErrorCode.USER_DATABASE_ERROR); }
}

/// 트래픽이 어느 정도 이상이 되면 해당 예외를 던지도록 설정함.

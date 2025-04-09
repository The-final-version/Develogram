package com.goorm.clonestagram.exception.user;

import lombok.Getter;

@Getter
public abstract class BusinessException extends RuntimeException {
	private final ErrorCode errorCode;
	protected BusinessException(ErrorCode code) {
		super(code.getMessage());
		this.errorCode = code;
	}
}

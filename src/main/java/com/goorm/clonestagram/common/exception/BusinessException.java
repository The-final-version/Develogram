package com.goorm.clonestagram.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
	private final ErrorCode errorCode;
	public BusinessException(ErrorCode code) {
		super(code.getMessage());
		this.errorCode = code;
	}
}

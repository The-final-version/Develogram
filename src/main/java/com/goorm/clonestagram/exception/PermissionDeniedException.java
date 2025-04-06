package com.goorm.clonestagram.exception;

public class PermissionDeniedException extends RuntimeException {

	public PermissionDeniedException() {
		super("해당 작업을 수행할 권한이 없습니다.");
	}

	public PermissionDeniedException(String message) {
		super(message);
	}
}

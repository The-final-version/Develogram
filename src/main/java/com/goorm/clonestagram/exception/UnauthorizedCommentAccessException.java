package com.goorm.clonestagram.exception;

public class UnauthorizedCommentAccessException extends RuntimeException {
    public UnauthorizedCommentAccessException() {
        super("댓글을 삭제할 권한이 없습니다.");
    }
}

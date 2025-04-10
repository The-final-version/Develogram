package com.goorm.clonestagram.common.exception;

import com.goorm.clonestagram.exception.CommentNotFoundException;
import com.goorm.clonestagram.exception.PostNotFoundException;

import com.goorm.clonestagram.exception.UnauthorizedCommentAccessException;
import com.goorm.clonestagram.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global Exception Handler
 * - 모든 컨트롤러 예외를 전역적으로 처리
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * IllegalArgumentException 처리 (400 Bad Request)
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException e) {
		return ResponseEntity.badRequest().body(
			ErrorResponseDto.builder().errorMessage(e.getMessage()).build()
		);
	}

	/**
	 * RuntimeException 처리 (500 Internal Server Error)
	 * ※ IllegalArgumentException 등 구체적 예외는 위에서 먼저 처리되므로 여기는 최종 catch-all 용
	 */
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorResponseDto> handleRuntimeException(RuntimeException e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
			ErrorResponseDto.builder().errorMessage(e.getMessage()).build()
		);
	}

	/**
	 * DTO Validation 실패 시 처리 (400 Bad Request)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException e) {
		return ResponseEntity.badRequest().body(
			ErrorResponseDto.builder().errorMessage(e.getMessage()).build()
		);
	}

	/**
	 * 댓글 없음 예외 처리 (404 Not Found)
	 */
	@ExceptionHandler(CommentNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleCommentNotFound(CommentNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("댓글을 찾을 수 없습니다");
		problem.setDetail(ex.getMessage());

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(PostNotFoundException.class)
	public ResponseEntity<ProblemDetail> handlePostNotFound(PostNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("게시글을 찾을 수 없습니다");
		problem.setDetail(ex.getMessage());

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}


	@ExceptionHandler(UnauthorizedCommentAccessException.class)
	public ResponseEntity<ProblemDetail> handleUnauthorizedCommentAccessException(
		UnauthorizedCommentAccessException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
		problem.setTitle("권한이 없습니다");
		problem.setDetail(ex.getMessage());

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
	}


	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<String> handleUserNotFound(UserNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	}


}

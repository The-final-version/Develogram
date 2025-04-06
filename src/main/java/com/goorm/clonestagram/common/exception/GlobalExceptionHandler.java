package com.goorm.clonestagram.common.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import com.goorm.clonestagram.exception.CommentNotFoundException;

/**
 * Global Exception Handler
 * - @RestControllerAdvice
 * - 모든 @RestController에서 발생하는 예외를 가로채서 공통적으로 처리
 * - 컨트롤러에 try-catch 문을 반복해서 작성하지 않고 여기서 한 번에 처리 가능
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * IllegalArgumentException 처리 메서드
	 * - 잘못된 파라미터, 요청 값 등에 의해서 IllegalArgumentException 발생 시
	 * - 에러 메시지를 클라이언트에게 JSON 응답으로 반환
	 *
	 * @param e 발생한 IllegalArgumentException
	 * @return 400 Bad Request 상태코드 + 에러 메시지
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDto> handleBadRequest(IllegalArgumentException e) {
		ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
			.errorMessage(e.getMessage()) // 발생한 예외의 메시지를 담음
			.build();
		return ResponseEntity.badRequest().body(errorResponseDto); // HTTP 400 상태로 응답
	}

	/**
	 * RuntimeException 처리 메서드
	 * - 런타임 오류 처리
	 * - 클라이언트에게 JSON 형식으로 에러 메시지 반환
	 *
	 * @param e 발생한 RuntimeException
	 * @return 400 Bad Request 상태코드 + 에러 메시지 (필요에 따라 500 응답으로 변경 가능)
	 */
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorResponseDto> handleServerError(RuntimeException e) {
		ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
			.errorMessage(e.getMessage())
			.build();
		return ResponseEntity.badRequest().body(errorResponseDto);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException e) {
		ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
			.errorMessage(e.getMessage())
			.build();
		return ResponseEntity.badRequest().body(errorResponseDto);
	}

	@ExceptionHandler(CommentNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleCommentNotFound(CommentNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("댓글을 찾을 수 없습니다");
		problem.setDetail(ex.getMessage());

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

}

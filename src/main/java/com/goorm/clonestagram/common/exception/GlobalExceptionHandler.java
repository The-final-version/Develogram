package com.goorm.clonestagram.common.exception;

import com.goorm.clonestagram.exception.CommentNotFoundException;
import com.goorm.clonestagram.exception.user.ApiResponse;
import com.goorm.clonestagram.exception.user.BusinessException;
import com.goorm.clonestagram.exception.user.ErrorCode;
import com.goorm.clonestagram.exception.user.error.DuplicateEmailException;
import com.goorm.clonestagram.exception.user.error.DuplicateLoginException;
import com.goorm.clonestagram.exception.user.error.InvalidCredentialsException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;


/**
 * Global Exception Handler
 * - 모든 컨트롤러 예외를 전역적으로 처리
 */
@Slf4j
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
	 * 댓글 없음 예외 처리 (404 Not Found)
	 */
	@ExceptionHandler(CommentNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleCommentNotFound(CommentNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("댓글을 찾을 수 없습니다");
		problem.setDetail(ex.getMessage());

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	/////////////////////////////////////////////////////////////////////////

	/* ===== 비즈니스 예외 ===== */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(
		BusinessException ex, HttpServletRequest req) {

		ErrorCode ec = ex.getErrorCode();
		log.warn("[BusinessException] {} - {}", ec.getCode(), ec.getMessage());
		return ResponseEntity.status(ec.getStatus())
			.body(ApiResponse.error(ec));
	}

	/* ===== Spring‑Security 인증 예외 ===== */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex) {

		ErrorCode ec;
		if (ex instanceof BadCredentialsException)            ec = ErrorCode.AUTH_INVALID_CREDENTIALS;
		else if (ex instanceof LockedException)               ec = ErrorCode.AUTH_ACCOUNT_LOCKED;
		else if (ex instanceof DisabledException)             ec = ErrorCode.AUTH_ACCOUNT_DISABLED;
		else if (ex instanceof CredentialsExpiredException)   ec = ErrorCode.AUTH_CREDENTIALS_EXPIRED;
		else                                                  ec = ErrorCode.AUTH_ACCESS_DENIED;

		return ResponseEntity.status(ec.getStatus())
			.body(ApiResponse.error(ec));
	}

	@ExceptionHandler(DuplicateLoginException.class)
	public ResponseEntity<ApiResponse<Void>> handleDuplicateLogin(DuplicateLoginException ex) {
		ErrorCode ec = ErrorCode.AUTH_DUPLICATE_LOGIN; // 필요한 에러 코드로 설정
		return ResponseEntity.status(ec.getStatus())
			.body(ApiResponse.error(ec));
	}

	/* ===== 권한 거부 ===== */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleDenied() {
		return ResponseEntity.status(ErrorCode.AUTH_ACCESS_DENIED.getStatus())
			.body(ApiResponse.error(ErrorCode.AUTH_ACCESS_DENIED));
	}

	/* ===== DTO 검증 실패 ===== */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValid(MethodArgumentNotValidException ex) {
		return ResponseEntity.badRequest()
			.body(ApiResponse.error(ErrorCode.USER_VALIDATION_GENERIC, ex.getBindingResult()));
	}

	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<String> handleDuplicateEmail(DuplicateEmailException e) {
		return ResponseEntity
			.status(HttpStatus.CONFLICT)          // 409
			.body(e.getMessage());
	}
	/* ===== 바인딩/파라미터 오류 ===== */
	@ExceptionHandler({
		MissingServletRequestParameterException.class,
		HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class,
		BindException.class })
	public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
		return ResponseEntity.badRequest()
			.body(ApiResponse.error(ErrorCode.USER_VALIDATION_GENERIC));
	}

	/* ===== 지원하지 않는 HTTP Method ===== */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethod() {
		return ResponseEntity.status(ErrorCode.AUTH_ACCESS_DENIED.getStatus())
			.body(ApiResponse.error(ErrorCode.AUTH_ACCESS_DENIED));
	}

	/* ===== JPA 제약조건 위반 ===== */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrity() {
		return ResponseEntity.status(ErrorCode.USER_DATABASE_ERROR.getStatus())
			.body(ApiResponse.error(ErrorCode.USER_DATABASE_ERROR));
	}

	/* ===== 알 수 없는 예외 ===== */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleOthers(Exception ex) {
		log.error("[Unhandled]", ex);
		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<String> handleInvalidCred(InvalidCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(ex.getMessage());
	}

}

package com.goorm.clonestagram.common.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.goorm.clonestagram.exception.CommentNotFoundException;
import com.goorm.clonestagram.exception.PostNotFoundException;

import com.goorm.clonestagram.exception.UnauthorizedCommentAccessException;
import com.goorm.clonestagram.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
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
		log.warn("잘못된 인자 예외 발생: {}", e.getMessage());
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

	/**
	 * 낙관적 락 충돌 예외 처리 핸들러
	 * @param ex ConcurrencyFailureException (Spring의 예외)
	 * @return 400 Bad Request 응답과 에러 메시지 (테스트 코드 호환성을 위해 변경)
	 */
	@ExceptionHandler(ConcurrencyFailureException.class)
	public ResponseEntity<ErrorResponseDto> handleConcurrencyFailure(ConcurrencyFailureException ex) {
		log.warn("동시성 충돌 발생: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponseDto.builder().errorMessage(ex.getMessage()).build());
	}

	/**
	 * 그 외 모든 예상치 못한 예외 처리
	 * @param ex Exception
	 * @return 500 Internal Server Error 응답과 에러 메시지
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleGeneralException(Exception ex) {
		log.error("예상치 못한 오류 발생", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
	}
	// /////////////////////////////////////////////////////////////////////////
	// /**
	//  * 1. BindException Handler
	//  * 폼 데이터(또는 쿼리 파라미터) 바인딩 과정에서 유효성 검증에 실패한 경우 발생하는 예외를 처리합니다.
	//  * HTTP 상태 코드: 400 (BAD_REQUEST)
	//  * 에러 코드: C-002 (INVALID_PARAMETER)
	//  * 에러 요약 메시지: ErrorCode.INVALID_PARAMETER.getMessage()
	//  * 에러 상세 메시지 목록: BindException 내부의 FieldError 정보를 기반으로 생성
	//  *
	//  * @param e BindException 객체
	//  * @return 오류 응답(ResponseEntity<ApiResponse<?>>)
	//  */
	// @ExceptionHandler(BindException.class)
	// protected ResponseEntity<ApiResponse<?>> handleBindException(BindException e) {
	// 	log.error("[handleBindException] 발생", e);
	// 	String summaryMessage = ErrorCode.INVALID_PARAMETER.getMessage();
	// 	List<String> detailList = new ArrayList<>();
	// 	for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
	// 		StringBuilder errorMsg = new StringBuilder();
	// 		errorMsg.append("필드 [").append(fieldError.getField()).append("]: ");
	// 		String[] codes = fieldError.getCodes();
	// 		boolean isMissing = false;
	// 		boolean isTypeMismatch = false;
	// 		if (codes != null) {
	// 			for (String code : codes) {
	// 				if (code != null) {
	// 					if (code.contains("NotNull") || code.contains("NotBlank") || code.contains("NotEmpty")) {
	// 						isMissing = true;
	// 					}
	// 					if (code.contains("typeMismatch")) {
	// 						isTypeMismatch = true;
	// 					}
	// 				}
	// 			}
	// 		}
	// 		if (isMissing) {
	// 			errorMsg.append("필수 입력값이 누락되었습니다. ");
	// 		}
	// 		if (isTypeMismatch) {
	// 			errorMsg.append("형식이 올바르지 않습니다. ");
	// 		}
	// 		errorMsg.append(fieldError.getDefaultMessage());
	// 		detailList.add(errorMsg.toString());
	// 	}
	// 	ApiResponse<?> errorResponse = ApiResponse.error(
	// 		ErrorCode.INVALID_PARAMETER.getCode(),
	// 		summaryMessage,
	// 		detailList,
	// 		ErrorCode.INVALID_PARAMETER.getHttpStatus().value(),
	// 		e.getBindingResult()
	// 	);
	// 	return ResponseEntity.status(ErrorCode.INVALID_PARAMETER.getHttpStatus()).body(errorResponse);
	// }
	//
	/**
	 * 2. MethodArgumentNotValidException Handler (RequestBody)
	 * JSON RequestBody로 들어오는 DTO의 유효성 검증 실패 시 발생하는 예외를 처리합니다.
	 * HTTP 상태 코드: 400 (BAD_REQUEST)
	 * 에러 코드: C-002 (INVALID_PARAMETER)
	 * 에러 요약 메시지: ErrorCode.INVALID_PARAMETER.getMessage()
	 * 에러 상세 메시지 목록: MethodArgumentNotValidException 내부의 FieldError 정보를 기반으로 생성
	 *
	 * @param e MethodArgumentNotValidException 객체
	 * @return 오류 응답(ResponseEntity<ApiResponse<?>>)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		log.error("[handleMethodArgumentNotValidException] 발생", e);
		String summaryMessage = ErrorCode.INVALID_PARAMETER.getMessage();
		List<String> detailList = new ArrayList<>();
		for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
			StringBuilder errorMsg = new StringBuilder();
			errorMsg.append("필드 [").append(fieldError.getField()).append("]: ");
			String[] codes = fieldError.getCodes();
			boolean isMissing = false;
			boolean isTypeMismatch = false;
			if (codes != null) {
				for (String code : codes) {
					if (code != null) {
						if (code.contains("NotNull") || code.contains("NotBlank") || code.contains("NotEmpty")) {
							isMissing = true;
						}
						if (code.contains("typeMismatch")) {
							isTypeMismatch = true;
						}
					}
				}
			}
			if (isMissing) {
				errorMsg.append("필수 입력값이 누락되었습니다. ");
			}
			if (isTypeMismatch) {
				errorMsg.append("형식이 올바르지 않습니다. ");
			}
			errorMsg.append(fieldError.getDefaultMessage());
			detailList.add(errorMsg.toString());
		}
		ApiResponse<?> errorResponse = ApiResponse.error(
			ErrorCode.INVALID_PARAMETER.getCode(),
			summaryMessage,
			detailList,
			ErrorCode.INVALID_PARAMETER.getHttpStatus().value(),
			e.getBindingResult()
		);
		return ResponseEntity.status(ErrorCode.INVALID_PARAMETER.getHttpStatus()).body(errorResponse);
	}
	//
	// /**
	//  * 3. MethodArgumentTypeMismatchException Handler (예: enum 바인딩 실패)
	//  * 파라미터 타입이 불일치할 때(예: enum 바인딩 실패) 발생하는 예외를 처리합니다.
	//  * HTTP 상태 코드: 400 (BAD_REQUEST)
	//  * 에러 코드: C-002 (INVALID_PARAMETER)
	//  * 에러 요약 메시지: ErrorCode.INVALID_PARAMETER.getMessage()
	//  * 에러 상세 메시지 목록: 파라미터 이름, 필요 타입, 예외 메시지 등
	//  *
	//  * @param e MethodArgumentTypeMismatchException 객체
	//  * @return 오류 응답(ResponseEntity<ApiResponse<?>>)
	//  */
	// @ExceptionHandler(MethodArgumentTypeMismatchException.class)
	// protected ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
	// 	log.error("[handleMethodArgumentTypeMismatchException] 발생", e);
	// 	String summaryMessage = ErrorCode.INVALID_PARAMETER.getMessage();
	// 	String detailMessage = String.format(
	// 		"파라미터 '%s'의 값이 적절하지 않습니다. (필요한 타입: %s). 상세: %s",
	// 		e.getName(),
	// 		(e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "알 수 없음"),
	// 		e.getMessage()
	// 	);
	// 	List<String> detailList = new ArrayList<>();
	// 	detailList.add(detailMessage);
	// 	ApiResponse<?> errorResponse = ApiResponse.error(
	// 		ErrorCode.INVALID_PARAMETER.getCode(),
	// 		summaryMessage,
	// 		detailList,
	// 		ErrorCode.INVALID_PARAMETER.getHttpStatus().value()
	// 	);
	// 	return ResponseEntity.status(ErrorCode.INVALID_PARAMETER.getHttpStatus()).body(errorResponse);
	// }
	//
	// /**
	//  * 4. HttpRequestMethodNotSupportedException Handler
	//  * 지원하지 않는 HTTP 메서드를 호출했을 때 발생하는 예외를 처리합니다.
	//  * HTTP 상태 코드: 405 (METHOD_NOT_ALLOWED)
	//  * 에러 코드: E-405 (METHOD_NOT_ALLOWED)
	//  * 에러 요약 메시지: ErrorCode.METHOD_NOT_ALLOWED.getMessage()
	//  * 에러 상세 메시지 목록: 요청 메서드, 지원 메서드 목록 등
	//  *
	//  * @param e HttpRequestMethodNotSupportedException 객체
	//  * @return 오류 응답(ResponseEntity<ApiResponse<?>>)
	//  */
	// @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	// protected ResponseEntity<ApiResponse<?>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
	// 	log.error("[handleHttpRequestMethodNotSupportedException] 발생", e);
	// 	String summaryMessage = ErrorCode.METHOD_NOT_ALLOWED.getMessage();
	// 	String detailMessage = String.format(
	// 		"요청한 메서드: [%s], 사용 가능한 메서드: [%s].",
	// 		e.getMethod(),
	// 		(e.getSupportedMethods() != null ? String.join(", ", e.getSupportedMethods()) : "없음")
	// 	);
	// 	List<String> detailList = new ArrayList<>();
	// 	detailList.add(detailMessage);
	// 	ApiResponse<?> errorResponse = ApiResponse.error(
	// 		ErrorCode.METHOD_NOT_ALLOWED.getCode(),
	// 		summaryMessage,
	// 		detailList,
	// 		ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus().value()
	// 	);
	// 	return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus()).body(errorResponse);
	// }
	//
	// /**
	//  * 5. HttpMessageNotReadableException Handler
	//  * 요청 본문이 누락되었거나(JSON 미제공 등) JSON 파싱에 실패했을 때 발생하는 예외를 처리합니다.
	//  * HTTP 상태 코드: 400 (BAD_REQUEST)
	//  * 에러 코드: C-003 (BODY_NOT_READABLE)
	//  * 에러 요약 메시지: ErrorCode.BODY_NOT_READABLE.getMessage()
	//  * 에러 상세 메시지 목록: 본문 누락 안내, JSON 파싱 오류 메시지 등
	//  *
	//  * @param e HttpMessageNotReadableException 객체
	//  * @return 오류 응답(ResponseEntity<ApiResponse<?>>)
	//  */
	// @ExceptionHandler(HttpMessageNotReadableException.class)
	// protected ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
	// 	log.error("[handleHttpMessageNotReadableException] 발생", e);
	// 	String summaryMessage = ErrorCode.BODY_NOT_READABLE.getMessage();
	// 	List<String> detailList = new ArrayList<>();
	// 	if(e.getMessage().contains("Required request body is missing")) {
	// 		detailList.add("요청 본문이 전혀 전달되지 않았습니다. JSON 형식의 요청 본문을 포함하여 필수 입력 데이터를 제공해 주십시오.");
	// 	} else {
	// 		detailList.add(String.format("요청 바디 형식 오류. 원인: %s", e.getMessage()));
	// 	}
	// 	ApiResponse<?> errorResponse = ApiResponse.error(
	// 		ErrorCode.BODY_NOT_READABLE.getCode(),
	// 		summaryMessage,
	// 		detailList,
	// 		ErrorCode.BODY_NOT_READABLE.getHttpStatus().value()
	// 	);
	// 	return ResponseEntity.status(ErrorCode.BODY_NOT_READABLE.getHttpStatus()).body(errorResponse);
	// }
	//
	// /**
	//  * 6. MissingServletRequestParameterException Handler
	//  * 요청 파라미터가 누락되었을 때 발생하는 예외를 처리합니다.
	//  * HTTP 상태 코드: 400 (BAD_REQUEST)
	//  * 에러 코드: C-002 (INVALID_PARAMETER)
	//  * 에러 요약 메시지: ErrorCode.INVALID_PARAMETER.getMessage()
	//  * 에러 상세 메시지 목록: 누락된 파라미터 이름 등
	//  *
	//  * @param e MissingServletRequestParameterException 객체
	//  * @return 오류 응답(ResponseEntity<ApiResponse<?>>)
	//  */
	// @ExceptionHandler(MissingServletRequestParameterException.class)
	// protected ResponseEntity<ApiResponse<?>> handleMissingServletRequestParameterException(org.springframework.web.bind.MissingServletRequestParameterException e) {
	// 	log.error("[handleMissingServletRequestParameterException] 발생", e);
	// 	String summaryMessage = ErrorCode.INVALID_PARAMETER.getMessage();
	// 	String detailMessage = String.format("요청 파라미터 '%s'가 누락되었습니다.", e.getParameterName());
	// 	List<String> detailList = new ArrayList<>();
	// 	detailList.add(detailMessage);
	// 	ApiResponse<?> errorResponse = ApiResponse.error(
	// 		ErrorCode.INVALID_PARAMETER.getCode(),
	// 		summaryMessage,
	// 		detailList,
	// 		ErrorCode.INVALID_PARAMETER.getHttpStatus().value()
	// 	);
	// 	return ResponseEntity.status(ErrorCode.INVALID_PARAMETER.getHttpStatus()).body(errorResponse);
	// }
	//
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
		log.error("BusinessException 발생: {}, 요청 URI: {}", ex.getMessage(), request.getRequestURI(), ex);

		String summaryMessage = ex.getMessage();
		List<String> detailList = new ArrayList<>();
		detailList.add("에러 메시지: " + ex.getMessage());

		ApiResponse<?> apiResponse = ApiResponse.error(
			ErrorCode.INVALID_PARAMETER.getCode(),
			summaryMessage,
			detailList,
			HttpStatus.BAD_REQUEST.value()
		);

		return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
	}
}

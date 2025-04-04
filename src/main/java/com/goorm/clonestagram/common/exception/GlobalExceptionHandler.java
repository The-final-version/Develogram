package com.goorm.clonestagram.common.exception;

import org.springframework.http.HttpStatus;
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
     * IllegalArgumentException → 400 반환
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException e){
        return ResponseEntity.badRequest().body(
                ErrorResponseDto.builder().errorMessage(e.getMessage()).build()
        );
    }

    /**
     * RuntimeException → 500 반환
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntimeException(RuntimeException e){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponseDto.builder().errorMessage(e.getMessage()).build()
        );
    }

    /**
     * 유효성 검증 실패 → 400 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(
                ErrorResponseDto.builder().errorMessage(e.getMessage()).build()
        );
    }
}

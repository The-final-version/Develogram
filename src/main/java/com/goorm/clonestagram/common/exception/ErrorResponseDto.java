package com.goorm.clonestagram.common.exception;

import lombok.*;

/**
 * 에러 응답을 위한 DTO
 * - errorMessage를 반환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
    private String errorMessage;
}
